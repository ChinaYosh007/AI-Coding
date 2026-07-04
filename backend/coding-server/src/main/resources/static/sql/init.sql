# 数据库初始化
# @author <a href="https://github.com/chinayosh007">china yosh</a>

-- 创建库
drop  database yupi_ai_code_monther;
create database if not exists yupi_ai_code_monther;

-- 切换库
use yupi_ai_code_monther;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    vipExpireTime datetime     null comment '会员过期时间',
    vipCode       varchar(128) null comment '会员兑换码',
    vipNumber     bigint       null comment '会员编号',
    shareCode     varchar(20)  DEFAULT NULL COMMENT '分享码',
    inviteUser    bigint       DEFAULT NULL COMMENT '邀请用户 id',
        UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

ALTER TABLE `user`
    ADD COLUMN `vipExpireTime` datetime NULL COMMENT 'VIP过期时间',
    ADD COLUMN `vipCode` varchar(128) NULL COMMENT 'VIP兑换码',
    ADD COLUMN `vipNumber` int NULL COMMENT 'VIP序号',
    ADD COLUMN `shareCode` varchar(64) NULL COMMENT '分享码',
    ADD COLUMN `inviteUser` bigint NULL COMMENT '邀请用户id';

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

alter table app add  version varchar(32) default '1.0.0' comment '版本号';
alter table app modify  version BIGINT  comment '版本号';
alter table app drop  version;

create table app_version
(
    id          bigint auto_increment comment 'id' primary key,
    appId       bigint                             not null comment '应用 id',
    version     bigint(32)                        not null comment '版本号',
    codeGenType varchar(64)                        not null comment '代码生成类型',
    sourcePath  varchar(1024)                      not null comment '生成代码目录',
    userMessage text                               null comment '用户本次生成消息',
    aiResponse  longtext                           null comment 'AI 本次生成结果',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    unique key uk_appId_version (appId, version),
    index idx_appId (appId)
) comment '应用代码版本' collate = utf8mb4_unicode_ci;
alter  table app_version modify column version bigint not null comment '版本号';
-- 对话历史表
create table if not exists app_collaboration
(
    id         bigint auto_increment comment 'id' primary key,
    appId      bigint                                 not null comment 'app id',
    userId     bigint                                 not null comment 'collaborator user id',
    role       varchar(32) default 'collaborator'     not null comment 'owner/collaborator/viewer',
    createTime datetime    default CURRENT_TIMESTAMP  not null comment 'create time',
    updateTime datetime    default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete   tinyint     default 0                  not null comment 'is delete',
    unique key uk_appId_userId (appId, userId),
    index idx_appId (appId),
    index idx_userId (userId)
) comment 'app collaboration' collate = utf8mb4_unicode_ci;

create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

