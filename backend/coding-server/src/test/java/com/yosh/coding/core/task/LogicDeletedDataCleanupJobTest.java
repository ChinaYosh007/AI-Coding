package com.yosh.coding.core.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogicDeletedDataCleanupJobTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldDeleteDirectoryInsideConfiguredRoot() throws IOException {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path target = Files.createDirectories(root.resolve("app/version"));
        Files.writeString(target.resolve("index.html"), "ok");

        Path safeTarget = LogicDeletedDataCleanupJob.resolveSafePath(root.toString(), target.toString());
        LogicDeletedDataCleanupJob.deleteRecursively(safeTarget);

        assertFalse(Files.exists(target));
    }

    @Test
    void shouldRejectRootAndOutsideDirectory() throws IOException {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path outside = Files.createDirectory(tempDirectory.resolve("outside"));

        assertThrows(IllegalStateException.class,
                () -> LogicDeletedDataCleanupJob.resolveSafePath(root.toString(), root.toString()));
        assertThrows(IllegalStateException.class,
                () -> LogicDeletedDataCleanupJob.resolveSafePath(root.toString(), outside.toString()));
    }
}
