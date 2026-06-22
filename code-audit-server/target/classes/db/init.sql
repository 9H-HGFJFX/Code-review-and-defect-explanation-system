-- ============================================================
-- 代码审查与缺陷解释系统 - 数据库初始化脚本
-- MySQL 8.0+
-- ============================================================

DROP DATABASE IF EXISTS `code_audit`;
CREATE DATABASE `code_audit` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `code_audit`;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT 学生 / TEACHER 教师 / ADMIN 管理员',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `real_name`   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 审查记录表
-- ============================================================
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审查记录ID',
    `user_id`      BIGINT       NOT NULL COMMENT '提交用户ID',
    `code_content` MEDIUMTEXT   NOT NULL COMMENT '原始代码内容',
    `file_name`    VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    `line_count`   INT          NOT NULL DEFAULT 0 COMMENT '代码行数',
    `issue_count`  INT          NOT NULL DEFAULT 0 COMMENT '问题总数',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待处理 / COMPLETED 已完成 / FAILED 审查失败',
    `error_msg`    VARCHAR(500) DEFAULT NULL COMMENT '审查失败原因',
    `review_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审查时间',
    `cost_ms`      BIGINT       DEFAULT NULL COMMENT '审查耗时（毫秒）',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_review_time` (`review_time`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查记录表';

-- ============================================================
-- 3. 审查问题表
-- ============================================================
DROP TABLE IF EXISTS `issue`;
CREATE TABLE `issue` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '问题ID',
    `review_id`   BIGINT       NOT NULL COMMENT '所属审查记录',
    `rule_id`     BIGINT       DEFAULT NULL COMMENT '触发的规则ID',
    `rule_name`   VARCHAR(100) DEFAULT NULL COMMENT '规则名称（冗余）',
    `category`    VARCHAR(20)  NOT NULL COMMENT '分类：STYLE / DEFECT / SECURITY',
    `severity`    VARCHAR(20)  NOT NULL COMMENT '严重级别：CRITICAL / ERROR / WARNING / SUGGESTION',
    `line_number` INT          NOT NULL DEFAULT 0 COMMENT '问题所在行号',
    `end_line`    INT          DEFAULT NULL COMMENT '结束行号',
    `col_number`  INT          DEFAULT NULL COMMENT '列号',
    `description` VARCHAR(500) NOT NULL COMMENT '问题描述',
    `suggestion`  TEXT         DEFAULT NULL COMMENT '修复建议',
    `code_before` TEXT         DEFAULT NULL COMMENT '修改前代码片段',
    `code_after`  TEXT         DEFAULT NULL COMMENT '修改后代码示例',
    `ai_explain`  TEXT         DEFAULT NULL COMMENT 'AI 缺陷解释（M3 生成）',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_review_id` (`review_id`),
    KEY `idx_severity` (`severity`),
    KEY `idx_category` (`category`),
    KEY `idx_review_severity` (`review_id`, `severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查问题表';

-- ============================================================
-- 4. 审查规则表
-- ============================================================
DROP TABLE IF EXISTS `rule`;
CREATE TABLE `rule` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `category`           VARCHAR(20)  NOT NULL COMMENT '分类：STYLE / DEFECT / SECURITY',
    `name`               VARCHAR(100) NOT NULL COMMENT '规则名称',
    `code`               VARCHAR(100) NOT NULL COMMENT '规则编码（程序内引用）',
    `pattern_type`       VARCHAR(20)  NOT NULL DEFAULT 'AST' COMMENT '匹配模式：REGEX 正则 / AST 语法树',
    `severity`           VARCHAR(20)  NOT NULL COMMENT '默认严重级别',
    `description`        VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
    `suggestion_template` TEXT        DEFAULT NULL COMMENT '修复建议模板',
    `executor_bean`      VARCHAR(100) DEFAULT NULL COMMENT '执行器 Bean 名（AST 模式）',
    `enabled`            TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用 / 0 禁用',
    `is_builtin`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否内置：1 内置 / 0 自定义',
    `class_id`           BIGINT       DEFAULT NULL COMMENT '班级 ID，为空代表全局公共规则',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_category` (`category`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审查规则表';

-- ============================================================
-- 5. 班级表
-- ============================================================
DROP TABLE IF EXISTS `class_group`;
CREATE TABLE `class_group` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '班级名称',
    `teacher_id`  BIGINT       NOT NULL COMMENT '管理教师ID',
    `description` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ============================================================
-- 6. 班级成员中间表
-- ============================================================
DROP TABLE IF EXISTS `class_user`;
CREATE TABLE `class_user` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `class_id`   BIGINT   NOT NULL COMMENT '班级ID',
    `student_id` BIGINT   NOT NULL COMMENT '学生ID',
    `join_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `is_deleted` TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级成员中间表';

-- ============================================================
-- 7. 初始化数据
-- ============================================================

-- 初始用户（密码均为 123456，BCrypt 加密）
-- BCrypt of "123456": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO `user` (`id`, `username`, `password`, `role`, `email`, `real_name`) VALUES
(1, 'admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',   'admin@codeaudit.com',   '系统管理员'),
(2, 'teacher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TEACHER', 'teacher@codeaudit.com', '张老师'),
(3, 'student', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', 'student@codeaudit.com', '李同学');

-- 内置规则（MVP + V1 增强 共 22 条）
INSERT INTO `rule` (`category`, `name`, `code`, `pattern_type`, `severity`, `description`, `suggestion_template`, `executor_bean`, `enabled`, `is_builtin`) VALUES
-- STYLE 规范
('STYLE', '类名大驼峰',     'NAMING_CLASS_UPPERCAMEL',     'AST', 'WARNING', '类名应采用大驼峰命名（UpperCamelCase）', '将类名改为大驼峰形式，例如 MyClass', 'namingChecker',     1, 1),
('STYLE', '方法名小驼峰',   'NAMING_METHOD_LOWERCAMEL',    'AST', 'WARNING', '方法名应采用小驼峰命名（lowerCamelCase）', '将方法名改为小驼峰形式，例如 myMethod', 'namingChecker',   1, 1),
('STYLE', '常量全大写',     'NAMING_CONSTANT_UPPER_SNAKE', 'AST', 'WARNING', '常量名应全大写+下划线分隔', '将常量名改为 MAX_VALUE 形式',           'namingChecker',     1, 1),
('STYLE', '单行字符超长',   'STYLE_LINE_TOO_LONG',         'AST', 'SUGGESTION', '单行字符数建议不超过 120', '将长行拆分为多行，提升可读性',       'formatChecker',  1, 1),
('STYLE', '方法过长',       'STYLE_METHOD_TOO_LONG',       'AST', 'WARNING', '单个方法行数建议不超过 80 行', '将方法拆分为多个职责单一的小方法',     'structureChecker', 1, 1),

-- DEFECT 缺陷
('DEFECT', '空指针风险',         'DEFECT_NULL_POINTER',     'AST', 'ERROR', '变量可能为 null 但未做非空判断就调用方法', '在使用前增加 if (obj != null) 判空，或使用 Optional',     'nullPointerDetector', 1, 1),
('DEFECT', '资源未关闭',         'DEFECT_RESOURCE_LEAK',    'AST', 'ERROR', 'IO 流、数据库连接等资源未在使用后关闭', '使用 try-with-resources 语句自动关闭资源',                   'resourceLeakDetector', 1, 1),
('DEFECT', '空 catch 块',        'DEFECT_EMPTY_CATCH',       'AST', 'WARNING', 'catch 块为空，会吞掉异常', '至少记录日志：log.error("xxx failed", e);',                 'emptyCatchDetector', 1, 1),
('DEFECT', '捕获过宽异常',       'DEFECT_BROAD_CATCH',       'AST', 'WARNING', '直接捕获 Exception 或 Throwable', '捕获具体异常类型，如 IOException、SQLException',           'broadCatchDetector', 1, 1),
('DEFECT', '条件恒为真',         'DEFECT_ALWAYS_TRUE',        'AST', 'ERROR',   'if/while 条件恒为 true（字面量 true、x==x 等）', '将条件修正为有效判断；若需无条件循环，使用 while(true) 并在体内显式 break', 'conditionDetector', 1, 1),
('DEFECT', '条件恒为假',         'DEFECT_ALWAYS_FALSE',       'AST', 'WARNING', 'if/while 条件恒为 false（字面量 false、x!=x 等）', '移除死分支；或将条件修正为有效判断',                            'conditionDetector', 1, 1),
('DEFECT', '潜在死循环',         'DEFECT_INFINITE_LOOP',      'AST', 'ERROR',   'while(true)/for(;;) 循环体内未发现 break/return 等出口', '在循环体内增加显式出口（break/return/throw/System.exit）',  'infiniteLoopDetector', 1, 1),
('DEFECT', '不安全类型转换',     'DEFECT_UNSAFE_CAST',        'AST', 'WARNING', '未经 instanceof 校验的强制类型转换，可能抛 ClassCastException', '在转换前使用 instanceof 校验；Java 16+ 用 Pattern Matching',     'unsafeCastDetector', 1, 1),
('DEFECT', '遍历时修改集合',     'DEFECT_CONCURRENT_MODIFICATION', 'AST', 'ERROR', '在 for-each 循环中调用 add/remove/put 等修改方法', '使用 Iterator 的 remove()；或先收集待删元素、循环结束后再统一修改', 'concurrentModDetector', 1, 1),
('DEFECT', '除零风险',           'DEFECT_DIVIDE_BY_ZERO',     'AST', 'ERROR',   '除法/取模运算的右操作数为字面量 0', '在执行前校验除数非 0，或使用 Math.floorDiv 等安全 API',           'divideByZeroDetector', 1, 1),
('DEFECT', '浮点等值比较',       'DEFECT_FLOAT_EQUALITY',     'AST', 'WARNING', '使用 == / != 比较 double/float 类型', '使用误差范围比较：Math.abs(a - b) < EPSILON',                       'floatEqualityDetector', 1, 1),

-- SECURITY 安全
('SECURITY', 'SQL 注入风险',    'SECURITY_SQL_INJECTION',  'REGEX', 'CRITICAL', '使用字符串拼接构造 SQL 语句，存在 SQL 注入风险', '使用 PreparedStatement 参数化查询',                          'sqlInjectionScanner',  1, 1),
('SECURITY', '硬编码密码',      'SECURITY_HARDCODED_SECRET', 'REGEX', 'CRITICAL', '代码中出现明文密码、密钥、Token 等敏感信息', '将敏感信息移至配置文件或环境变量，配置项加密存储',             'hardcodedSecretScanner', 1, 1),
('SECURITY', 'XSS 风险',        'SECURITY_XSS',            'REGEX', 'CRITICAL', '未转义的用户输入直接输出到页面', '使用 Thymeleaf、Vue 等模板的自动转义，或对输出做 HTML 转义',  'xssScanner', 1, 1),
('SECURITY', '命令注入',        'SECURITY_COMMAND_INJECTION','REGEX', 'CRITICAL', '用户输入拼接到 Runtime.exec() 等命令执行函数', '避免拼接用户输入到命令，使用 ProcessBuilder 加白名单校验',     'commandInjectionScanner', 1, 1),
('SECURITY', '路径遍历',        'SECURITY_PATH_TRAVERSAL', 'REGEX', 'CRITICAL', '未校验的用户输入直接用于文件路径构造', '白名单校验 + 路径规范化（resolve+normalize+startsWith）',     'pathTraversalScanner', 1, 1),
('SECURITY', '敏感信息日志',    'SECURITY_SENSITIVE_LOG',  'REGEX', 'WARNING',  '日志/输出语句打印了密码/Token/身份证等敏感字段', '在日志中脱敏：log.info("token={}***", token.substring(0,4))', 'sensitiveLogScanner', 1, 1),
('SECURITY', '不安全加密',      'SECURITY_INSECURE_CRYPTO','REGEX', 'CRITICAL', '使用 MD5/SHA-1/DES/3DES 等已破解或不安全的算法', '密码用 BCrypt/Argon2；对称加密用 AES-GCM；摘要用 SHA-256+',  'insecureCryptoScanner', 1, 1),
('SECURITY', '反序列化风险',    'SECURITY_UNSAFE_DESERIALIZE', 'REGEX', 'CRITICAL', '对不可信输入使用 Java 原生/XML/Jackson default typing/YAML 反序列化', '优先用 JSON 等安全格式；必须用 Java 反序列化时加 ObjectInputFilter 白名单', 'unsafeDeserializeScanner', 1, 1);
