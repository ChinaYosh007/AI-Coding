package com.yosh.coding.core.task;

import com.yosh.coding.core.AiCodeGeneratorFacade;
import com.yosh.coding.mapper.AppMapper;
import com.yosh.coding.mapper.AppVersionMapper;
import com.yosh.coding.service.LogicDeletedDataCleanupService;
import com.yosh.coding.service.OssUploadService;
import com.yosh.model.constants.AppConstant;
import com.yosh.model.entity.App;
import com.yosh.model.entity.AppVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogicDeletedDataCleanupJob {

    private static final String LOCK_KEY = "job:logic-deleted-data-cleanup";
    private static final String GOOD_APP_CACHE = "good_app_page";
    private static final int RETENTION_DAYS = 0;
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_BATCH_COUNT = 100;

    private final AppMapper appMapper;
    private final AppVersionMapper appVersionMapper;
    private final RedissonClient redissonClient;
    private final LogicDeletedDataCleanupService cleanupService;
    private final OssUploadService ossUploadService;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;
    private final CacheManager cacheManager;

    /**
     * 默认每天凌晨三点执行，可通过 cleanup.logic-deleted.cron 覆盖。
     */
    @Scheduled(
            cron = "${cleanup.logic-deleted.cron:0 0 3 * * ?}",
            zone = "${cleanup.logic-deleted.zone:Asia/Shanghai}"
    )
    public void cleanup() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean locked = false;

        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                log.info("其他实例正在执行逻辑删除数据清理任务");
                return;
            }

            LocalDateTime expireTime = LocalDateTime.now().minusDays(RETENTION_DAYS);
            long lastId = 0L;
            int successCount = 0;
            int failureCount = 0;

            log.info("逻辑删除数据清理开始，过期时间={}", expireTime);
            for (int batchIndex = 0; batchIndex < MAX_BATCH_COUNT; batchIndex++) {
                List<App> apps = appMapper.selectExpiredLogicDeletedApps(expireTime, lastId, BATCH_SIZE);
                if (apps.isEmpty()) {
                    break;
                }

                for (App app : apps) {
                    lastId = app.getId();
                    try {
                        cleanupApp(app);
                        successCount++;
                    } catch (Exception exception) {
                        failureCount++;
                        log.error("清理逻辑删除应用失败，等待下次重试，appId={}", app.getId(), exception);
                    }
                }

                if (apps.size() < BATCH_SIZE) {
                    break;
                }
            }

            log.info("逻辑删除数据清理完成，成功={}，失败={}", successCount, failureCount);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("逻辑删除数据清理任务被中断", exception);
        } catch (Exception exception) {
            log.error("逻辑删除数据清理任务失败", exception);
        } finally {
            unlockSafely(lock, locked);
        }
    }

    private void cleanupApp(App app) throws IOException {
        long appId = app.getId();
        List<AppVersion> appVersions = appVersionMapper.selectAllByAppId(appId);

        List<Path> sourceDirectories = appVersions.stream()
                .map(AppVersion::getSourcePath)
                .map(sourcePath -> resolveSafePath(AppConstant.CODE_OUTPUT_ROOT_DIR, sourcePath))
                .toList();

        Path deployDirectory = null;
        if (app.getDeployKey() != null && !app.getDeployKey().isBlank()) {
            Path deployPath = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR).resolve(app.getDeployKey());
            deployDirectory = resolveSafePath(AppConstant.CODE_DEPLOY_ROOT_DIR, deployPath.toString());
        }

        for (Path sourceDirectory : sourceDirectories) {
            deleteRecursively(sourceDirectory);
        }
        if (deployDirectory != null) {
            deleteRecursively(deployDirectory);
        }

        deleteCoverIfUnreferenced(app);
        aiCodeGeneratorFacade.clearAppMemory(appId);
        clearGoodAppCache();
        cleanupService.physicalDeleteAppData(appId);
    }

    private void deleteCoverIfUnreferenced(App app) {
        String cover = app.getCover();
        if (cover == null || cover.isBlank()) {
            return;
        }
        if (appMapper.countOtherCoverReferences(cover, app.getId()) == 0) {
            ossUploadService.deleteFileByUrl(cover);
        } else {
            log.info("应用封面仍被其他应用引用，跳过 OSS 删除，appId={}", app.getId());
        }
    }

    private void clearGoodAppCache() {
        Cache cache = cacheManager.getCache(GOOD_APP_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    static Path resolveSafePath(String rootDirectory, String targetDirectory) {
        if (targetDirectory == null || targetDirectory.isBlank()) {
            throw new IllegalStateException("待删除目录不能为空");
        }

        Path root = Path.of(rootDirectory).toAbsolutePath().normalize();
        Path target = Path.of(targetDirectory).toAbsolutePath().normalize();
        if (target.equals(root) || !target.startsWith(root)) {
            throw new IllegalStateException("拒绝删除生成目录之外的路径：" + target);
        }
        return target;
    }

    static void deleteRecursively(Path target) throws IOException {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void unlockSafely(RLock lock, boolean locked) {
        if (!locked) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RedisException exception) {
            log.warn("释放逻辑删除数据清理锁失败", exception);
        }
    }
}
