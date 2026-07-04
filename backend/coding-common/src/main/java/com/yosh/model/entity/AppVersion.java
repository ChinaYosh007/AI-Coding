package com.yosh.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用代码版本 实体类。
 *
 * @author china_yosh
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version")
public class AppVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 应用 id
     */
    @Column("appId")
    private Long appId;

    /**
     * 版本号
     */
    @Column("version")
    private Long version;

    /**
     * 代码生成类型
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 生成代码目录
     */
    @Column("sourcePath")
    private String sourcePath;

    /**
     * 用户本次生成消息
     */
    @Column("userMessage")
    private String userMessage;

    /**
     * AI 本次生成结果
     */
    @Column("aiResponse")
    private String aiResponse;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
