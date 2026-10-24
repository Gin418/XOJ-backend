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

-- 讨论表
create table if not exists discussion
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    questionId bigint                             null comment '题目 id',
    parentId   bigint                             null comment '父评论 id（null 表示一级评论/主贴，回复指向父级）',
    replyUserId bigint                            null comment '被回复用户 id（回复时冗余父评论作者，null 表示一级评论）',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_questionId (questionId),
    index idx_parentId (parentId)
) comment '讨论' collate = utf8mb4_unicode_ci;

-- 讨论点赞表（硬删除）
create table if not exists discussion_thumb
(
    id           bigint auto_increment comment 'id' primary key,
    discussionId bigint                             not null comment '讨论 id',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_discussionId (discussionId),
    index idx_userId (userId)
) comment '讨论点赞';

-- 讨论收藏表（硬删除）
create table if not exists discussion_favour
(
    id           bigint auto_increment comment 'id' primary key,
    discussionId bigint                             not null comment '讨论 id',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_discussionId (discussionId),
    index idx_userId (userId)
) comment '讨论收藏';

-- 竞赛表
create table if not exists competition
(
    id               bigint auto_increment comment 'id' primary key,
    competitionTitle varchar(256)                       null comment '竞赛标题',
    description      text                               null comment '描述',
    startTime        varchar(52)                        null comment '开始时间',
    endTime          varchar(52)                        null comment '结束时间',
    freezeTime       varchar(52)                        null comment '封榜时间（封榜后排名不再更新）',
    allowSubmitAfterEnd tinyint  default 0             not null comment '是否允许赛后提交（0 不允许、1 允许）',
    isRevealed       tinyint  default 0                 not null comment '是否揭榜（0 未揭榜、1 已揭榜）',
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
    unique key uk_user_competition (usenerId, competitionId)
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

-- 存量库升级：竞赛表新增封榜时间字段
alter table competition
    add column freezeTime varchar(52) null comment '封榜时间（封榜后排名不再更新）' after endTime;

-- 存量库升级：竞赛表新增是否允许赛后提交字段
alter table competition
    add column allowSubmitAfterEnd tinyint default 0 not null comment '是否允许赛后提交（0 不允许、1 允许）' after freezeTime;

-- 存量库升级：帖子表重命名为讨论表（须先执行，后续升级语句均作用于讨论表）
rename table post to discussion;
rename table post_thumb to discussion_thumb;
rename table post_favour to discussion_favour;

-- 存量库升级：讨论点赞表 postId 字段重命名为 discussionId
alter table discussion_thumb
    change column postId discussionId bigint not null comment '讨论 id';
alter table discussion_thumb
    drop index idx_postId,
    add index idx_discussionId (discussionId);

-- 存量库升级：讨论收藏表 postId 字段重命名为 discussionId
alter table discussion_favour
    change column postId discussionId bigint not null comment '讨论 id';
alter table discussion_favour
    drop index idx_postId,
    add index idx_discussionId (discussionId);

-- 存量库升级：讨论表新增题目 id 字段及索引
alter table discussion
    add column questionId bigint null comment '题目 id' after content;
alter table discussion
    add index idx_questionId (questionId);

-- 存量库升级：讨论表新增父评论 id 字段及索引（回复功能，两级楼中楼）
alter table discussion
    add column parentId bigint null comment '父评论 id（null 表示一级评论/主贴，回复指向父级）' after questionId;
alter table discussion
    add index idx_parentId (parentId);

-- 存量库升级：讨论表新增被回复用户 id 字段（回复展示“回复 @xxx”用）
alter table discussion
    add column replyUserId bigint null comment '被回复用户 id（回复时冗余父评论作者，null 表示一级评论）' after parentId;

-- 存量库升级：讨论表删除标签字段
alter table discussion
    drop column tags;

-- 消息表（系统通知 / 私信）
create table if not exists message
(
    id         bigint auto_increment comment 'id' primary key,
    type       tinyint   default 0                 not null comment '消息类型：0 系统通知、1 私信',
    title      varchar(256)                        null comment '标题（系统通知使用）',
    content    text                                not null comment '内容',
    senderId   bigint                              null comment '发送用户 id（私信使用，通知为 null）',
    receiverId bigint                              null comment '接收用户 id（广播通知为 null，不落库）',
    isRead     tinyint   default 0                 not null comment '是否已读：0 未读、1 已读',
    createTime datetime  default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint   default 0                 not null comment '是否删除',
    index idx_receiverId (receiverId),
    index idx_senderId (senderId),
    index idx_type (type)
) comment '消息（系统通知/私信）' collate = utf8mb4_unicode_ci;

-- 私信会话表（每个用户与一位好友一条会话记录，发送私信或打开会话时创建/更新）
create table if not exists conversation
(
    id            bigint                             not null comment 'id' primary key,
    userId        bigint                             not null comment '会话所属用户 id',
    friendId      bigint                             not null comment '对方用户 id',
    lastMessageId bigint                             null comment '最后一条消息 id（冗余，发送私信时更新）',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    unique index uk_user_friend (userId, friendId),
    index idx_userId (userId)
) comment '私信会话' collate = utf8mb4_unicode_ci;

-- 存量库升级：竞赛表新增是否揭榜字段
alter table competition
    add column isRevealed tinyint default 0 not null comment '是否揭榜（0 未揭榜、1 已揭榜）' after allowSubmitAfterEnd;

