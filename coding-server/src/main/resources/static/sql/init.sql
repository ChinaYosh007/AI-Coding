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
