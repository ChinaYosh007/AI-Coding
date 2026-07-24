package com.yosh.coding.artificalIntelligence.model.message;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开发服务器消息——代码生成完成后，后端启动 npm run dev 并向
 * 前端推送预览 URL，前端可用 iframe 加载实现热更新预览。
 */
@Data
@NoArgsConstructor
public class DevServerMessage {
    private String type = "dev_server";
    /** 生成项目开发服务器 URL，如 http://127.0.0.1:5200 */
    private String url;

    public DevServerMessage(String url) {
        this.url = url;
    }
}
