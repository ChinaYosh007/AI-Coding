package com.yosh.coding.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.yosh.model.entity.AppVersion;
import com.yosh.coding.service.AppVersionService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 应用代码版本 控制层。
 *
 * @author china_yosh
 * @since 2026-07-04
 */
@RestController
@RequestMapping("/appVersion")
public class AppVersionController {

    @Autowired
    private AppVersionService appVersionService;

    /**
     * 保存应用代码版本。
     *
     * @param appVersion 应用代码版本
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody AppVersion appVersion) {
        return appVersionService.save(appVersion);
    }

    /**
     * 根据主键删除应用代码版本。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return appVersionService.removeById(id);
    }

    /**
     * 根据主键更新应用代码版本。
     *
     * @param appVersion 应用代码版本
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody AppVersion appVersion) {
        return appVersionService.updateById(appVersion);
    }

    /**
     * 查询所有应用代码版本。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<AppVersion> list() {
        return appVersionService.list();
    }

    /**
     * 根据主键获取应用代码版本。
     *
     * @param id 应用代码版本主键
     * @return 应用代码版本详情
     */
    @GetMapping("getInfo/{id}")
    public AppVersion getInfo(@PathVariable Long id) {
        return appVersionService.getById(id);
    }

    /**
     * 分页查询应用代码版本。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<AppVersion> page(Page<AppVersion> page) {
        return appVersionService.page(page);
    }

}
