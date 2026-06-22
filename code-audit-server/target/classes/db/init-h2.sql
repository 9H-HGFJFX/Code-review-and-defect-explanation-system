-- ============================================================
-- 代码审查与缺陷解释系统 - H2 兼容的数据库初始化脚本
-- 与 MySQL 版差异：去 ENGINE/CHARSET 子句，MEDIUMTEXT → CLOB
-- ============================================================

DROP TABLE IF EXISTS "user";
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS issue;
DROP TABLE IF EXISTS rule;
DROP TABLE IF EXISTS class_group;
DROP TABLE IF EXISTS class_user;

-- 1. 用户表
CREATE TABLE "user" (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    email       VARCHAR(100) DEFAULT NULL,
    real_name   VARCHAR(50)  DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_username UNIQUE (username)
);

-- 2. 审查记录表
CREATE TABLE review (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    code_content CLOB         NOT NULL,
    file_name    VARCHAR(255) DEFAULT NULL,
    line_count   INT          NOT NULL DEFAULT 0,
    issue_count  INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_msg    VARCHAR(500) DEFAULT NULL,
    review_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cost_ms      BIGINT       DEFAULT NULL,
    is_deleted   TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 3. 审查问题表
CREATE TABLE issue (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL,
    rule_id     BIGINT       DEFAULT NULL,
    rule_name   VARCHAR(100) DEFAULT NULL,
    category    VARCHAR(20)  NOT NULL,
    severity    VARCHAR(20)  NOT NULL,
    line_number INT          NOT NULL DEFAULT 0,
    end_line    INT          DEFAULT NULL,
    col_number  INT          DEFAULT NULL,
    description VARCHAR(500) NOT NULL,
    suggestion  CLOB         DEFAULT NULL,
    code_before CLOB         DEFAULT NULL,
    code_after  CLOB         DEFAULT NULL,
    ai_explain  CLOB         DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 4. 审查规则表
CREATE TABLE rule (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    category           VARCHAR(20)  NOT NULL,
    name               VARCHAR(100) NOT NULL,
    code               VARCHAR(100) NOT NULL,
    pattern_type       VARCHAR(20)  NOT NULL DEFAULT 'AST',
    severity           VARCHAR(20)  NOT NULL,
    description        VARCHAR(500) DEFAULT NULL,
    suggestion_template CLOB        DEFAULT NULL,
    executor_bean      VARCHAR(100) DEFAULT NULL,
    enabled            TINYINT      NOT NULL DEFAULT 1,
    is_builtin         TINYINT      NOT NULL DEFAULT 0,
    class_id           BIGINT       DEFAULT NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_name UNIQUE (name),
    CONSTRAINT uk_rule_code UNIQUE (code)
);

-- 5. 班级表
CREATE TABLE class_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    teacher_id  BIGINT       NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 6. 班级成员中间表
CREATE TABLE class_user (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    class_id   BIGINT   NOT NULL,
    student_id BIGINT   NOT NULL,
    join_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_student UNIQUE (class_id, student_id)
);

-- 7. 初始化数据 - 用户
-- BCrypt of "123456": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO "user" (id, username, password, role, email, real_name) VALUES
(1, 'admin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',   'admin@codeaudit.com',   '系统管理员'),
(2, 'teacher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TEACHER', 'teacher@codeaudit.com', '张老师'),
(3, 'student', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', 'student@codeaudit.com', '李同学');

-- 8. 内置规则（22 条）
INSERT INTO rule (category, name, code, pattern_type, severity, description, suggestion_template, executor_bean, enabled, is_builtin) VALUES
-- STYLE 规范
('STYLE', '类名大驼峰',        'NAMING_CLASS_UPPERCAMEL',      'AST',   'WARNING',     '类名应采用大驼峰命名（UpperCamelCase）',                          '将类名改为大驼峰形式，例如 MyClass',                                  'namingChecker',             1, 1),
('STYLE', '方法名小驼峰',      'NAMING_METHOD_LOWERCAMEL',     'AST',   'WARNING',     '方法名应采用小驼峰命名（lowerCamelCase）',                       '将方法名改为小驼峰形式，例如 myMethod',                              'namingChecker',             1, 1),
('STYLE', '常量全大写',        'NAMING_CONSTANT_UPPER_SNAKE',  'AST',   'WARNING',     '常量名应全大写+下划线分隔',                                      '将常量名改为 MAX_VALUE 形式',                                        'namingChecker',             1, 1),
('STYLE', '单行字符超长',      'STYLE_LINE_TOO_LONG',          'AST',   'SUGGESTION',  '单行字符数建议不超过 120',                                       '将长行拆分为多行，提升可读性',                                      'formatChecker',            1, 1),
('STYLE', '方法过长',          'STYLE_METHOD_TOO_LONG',        'AST',   'WARNING',     '单个方法行数建议不超过 80 行',                                   '将方法拆分为多个职责单一的小方法',                                  'structureChecker',         1, 1),

-- DEFECT 缺陷
('DEFECT', '空指针风险',         'DEFECT_NULL_POINTER',          'AST',   'ERROR',       '变量可能为 null 但未做非空判断就调用方法',                       '在使用前增加 if (obj != null) 判空，或使用 Optional',              'nullPointerDetector',      1, 1),
('DEFECT', '资源未关闭',         'DEFECT_RESOURCE_LEAK',         'AST',   'ERROR',       'IO 流、数据库连接等资源未在使用后关闭',                         '使用 try-with-resources 语句自动关闭资源',                          'resourceLeakDetector',     1, 1),
('DEFECT', '空 catch 块',        'DEFECT_EMPTY_CATCH',           'AST',   'WARNING',     'catch 块为空，会吞掉异常',                                       '至少记录日志：log.error("xxx failed", e);',                       'emptyCatchDetector',       1, 1),
('DEFECT', '捕获过宽异常',       'DEFECT_BROAD_CATCH',           'AST',   'WARNING',     '直接捕获 Exception 或 Throwable',                                '捕获具体异常类型，如 IOException、SQLException',                  'broadCatchDetector',       1, 1),
('DEFECT', '条件恒为真',         'DEFECT_ALWAYS_TRUE',           'AST',   'ERROR',       'if/while 条件恒为 true（字面量 true、x==x 等）',                '将条件修正为有效判断；若需无条件循环，使用 while(true) 并在体内显式 break', 'conditionDetector',        1, 1),
('DEFECT', '条件恒为假',         'DEFECT_ALWAYS_FALSE',          'AST',   'WARNING',     'if/while 条件恒为 false（字面量 false、x!=x 等）',               '移除死分支；或将条件修正为有效判断',                               'conditionDetector',        1, 1),
('DEFECT', '潜在死循环',         'DEFECT_INFINITE_LOOP',         'AST',   'ERROR',       'while(true)/for(;;) 循环体内未发现 break/return 等出口',         '在循环体内增加显式出口（break/return/throw/System.exit）',         'infiniteLoopDetector',     1, 1),
('DEFECT', '不安全类型转换',     'DEFECT_UNSAFE_CAST',           'AST',   'WARNING',     '未经 instanceof 校验的强制类型转换，可能抛 ClassCastException',  '在转换前使用 instanceof 校验；Java 16+ 用 Pattern Matching',        'unsafeCastDetector',       1, 1),
('DEFECT', '遍历时修改集合',     'DEFECT_CONCURRENT_MODIFICATION','AST',   'ERROR',       '在 for-each 循环中调用 add/remove/put 等修改方法',              '使用 Iterator 的 remove()；或先收集待删元素、循环结束后再统一修改', 'concurrentModDetector',    1, 1),
('DEFECT', '除零风险',           'DEFECT_DIVIDE_BY_ZERO',        'AST',   'ERROR',       '除法/取模运算的右操作数为字面量 0',                              '在执行前校验除数非 0，或使用 Math.floorDiv 等安全 API',          'divideByZeroDetector',     1, 1),
('DEFECT', '浮点等值比较',       'DEFECT_FLOAT_EQUALITY',        'AST',   'WARNING',     '使用 == / != 比较 double/float 类型',                           '使用误差范围比较：Math.abs(a - b) < EPSILON',                       'floatEqualityDetector',    1, 1),

-- SECURITY 安全
('SECURITY', 'SQL 注入风险',    'SECURITY_SQL_INJECTION',       'REGEX', 'CRITICAL',    '使用字符串拼接构造 SQL 语句，存在 SQL 注入风险',                 '使用 PreparedStatement 参数化查询',                                 'sqlInjectionScanner',      1, 1),
('SECURITY', '硬编码密码',      'SECURITY_HARDCODED_SECRET',    'REGEX', 'CRITICAL',    '代码中出现明文密码、密钥、Token 等敏感信息',                    '将敏感信息移至配置文件或环境变量，配置项加密存储',                 'hardcodedSecretScanner',   1, 1),
('SECURITY', 'XSS 风险',        'SECURITY_XSS',                 'REGEX', 'CRITICAL',    '未转义的用户输入直接输出到页面',                                '使用 Thymeleaf、Vue 等模板的自动转义，或对输出做 HTML 转义',        'xssScanner',               1, 1),
('SECURITY', '命令注入',        'SECURITY_COMMAND_INJECTION',   'REGEX', 'CRITICAL',    '用户输入拼接到 Runtime.exec() 等命令执行函数',                   '避免拼接用户输入到命令，使用 ProcessBuilder 加白名单校验',          'commandInjectionScanner',  1, 1),
('SECURITY', '路径遍历',        'SECURITY_PATH_TRAVERSAL',      'REGEX', 'CRITICAL',    '未校验的用户输入直接用于文件路径构造',                          '白名单校验 + 路径规范化（resolve+normalize+startsWith）',         'pathTraversalScanner',     1, 1),
('SECURITY', '敏感信息日志',    'SECURITY_SENSITIVE_LOG',       'REGEX', 'WARNING',     '日志/输出语句打印了密码/Token/身份证等敏感字段',                '在日志中脱敏：log.info("token={}***", token.substring(0,4))',     'sensitiveLogScanner',      1, 1),
('SECURITY', '不安全加密',      'SECURITY_INSECURE_CRYPTO',     'REGEX', 'CRITICAL',    '使用 MD5/SHA-1/DES/3DES 等已破解或不安全的算法',               '密码用 BCrypt/Argon2；对称加密用 AES-GCM；摘要用 SHA-256+',        'insecureCryptoScanner',    1, 1),
('SECURITY', '反序列化风险',    'SECURITY_UNSAFE_DESERIALIZE',  'REGEX', 'CRITICAL',    '对不可信输入使用 Java 原生/XML/Jackson default typing/YAML 反序列化', '优先用 JSON 等安全格式；必须用 Java 反序列化时加 ObjectInputFilter 白名单', 'unsafeDeserializeScanner', 1, 1);
