# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://yupi.icu">编程导航知识星球</a>

-- 创建库
create database if not exists yuoj;

-- 切换库
use yuoj;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    phone        varchar(32)                            null comment '手机号',
    email        varchar(52)                            null comment '邮箱',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 题目表
create table if not exists question
(
    id          bigint auto_increment comment 'id' primary key,
    customId    varchar(52) default ''             not null comment '题号',
    title       varchar(512)                       null comment '标题',
    content     text                               null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    description text                               null comment '题目说明',
    submitNum   int      default 0                 not null comment '题目提交数',
    acceptedNum int      default 0                 not null comment '题目通过数',
    judgeCase   text                               null comment '判题用例（json 数组）',
    judgeConfig text                               null comment '判题配置（json 对象）',
    inOutPath   varchar(512)                       null comment '用例文件路径',
    thumbNum    int      default 0                 not null comment '点赞数',
    favourNum   int      default 0                 not null comment '收藏数',
    userId      bigint                             not null comment '创建用户 id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '题目' collate = utf8mb4_unicode_ci;

-- 题目提交表
create table if not exists question_submit
(
    id         bigint auto_increment comment 'id' primary key,
    language   varchar(128)                       not null comment '编程语言',
    code       text                               not null comment '用户代码',
    judgeInfo  text                               null comment '判题信息（json 对象）',
    codeSize   int      default 0                 not null comment '代码大小',
    questionId bigint                             not null comment '题目 id',
    competitionId bigint                          null comment '竞赛 id（null 表示题库提交）',
    status     varchar(52) default ''             not null comment '判题状态',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_questionId (questionId),
    index idx_competitionId (competitionId),
    index idx_userId (userId)
) comment '题目提交（题库/竞赛共用）';

-- 帖子表
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    tags       varchar(1024)                      null comment '标签列表（json 数组）',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '帖子' collate = utf8mb4_unicode_ci;

-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子点赞';

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子收藏';

-- 竞赛表
create table if not exists competition
(
    id               bigint auto_increment comment 'id' primary key,
    competitionTitle varchar(256)                       null comment '竞赛标题',
    description      text                               null comment '描述',
    startTime        varchar(52)                        null comment '开始时间',
    endTime          varchar(52)                        null comment '结束时间',
    questionIds      varchar(1024)                      null comment '题目 id 列表（英文逗号分隔）',
    status           varchar(52)  default ''            not null comment '状态',
    userId           bigint                             null comment '创建用户 id',
    createTime       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete         tinyint  default 0                 not null comment '是否删除'
) comment '竞赛' collate = utf8mb4_unicode_ci;

-- 用户竞赛关系表（用户参加了哪些竞赛，硬删除）
create table if not exists user_competition
(
    id            bigint auto_increment comment 'id' primary key,
    competitionId bigint                             not null comment '竞赛 id',
    userId        bigint                             not null comment '用户 id',
    penaltyTime   bigint   default 0                 not null comment '罚时',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_competitionId (competitionId),
    index idx_userId (userId),
    unique key uk_user_competition (userId, competitionId)
) comment '用户竞赛关系' collate = utf8mb4_unicode_ci;

-- 竞赛题目关系表（竞赛包含哪些题目，硬删除）
create table if not exists competition_question
(
    id            bigint auto_increment comment 'id' primary key,
    competitionId bigint                             not null comment '竞赛 id',
    questionId    bigint                             not null comment '题目 id',
    questionOrder  int      default 1                 not null comment '题目顺序（题号 A、B、C……）',
    submitNum           int      default 0           not null comment '提交数',
    acceptedNum         int      default 0           not null comment '通过数',
    firstAcceptedUserId bigint                       null comment '第一个通过的用户 id',
    firstAcceptedTime   datetime                     null comment '首个通过用户的提交时间',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_competitionId (competitionId),
    index idx_questionId (questionId),
    unique key uk_competition_question (competitionId, questionId)
) comment '竞赛题目关系' collate = utf8mb4_unicode_ci;

-- 存量库升级：用户表新增手机号字段
alter table user
    add column phone varchar(32) null comment '手机号' after userPassword;

-- 存量库升级：用户表新增邮箱字段
alter table user
    add column email varchar(256) null comment '邮箱' after phone;

