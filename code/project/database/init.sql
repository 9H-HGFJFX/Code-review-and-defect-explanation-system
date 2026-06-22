-- ============================================
-- 代码审查与缺陷解释系统 - 数据库初始化脚本
-- 数据库版本: MySQL 8.0+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS code_review_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE code_review_db;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT学生/TEACHER教师/ADMIN管理员',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1已删除',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 审查记录表
-- ============================================
DROP TABLE IF EXISTS review;
CREATE TABLE review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    code_content TEXT NOT NULL COMMENT '原始代码内容',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    line_count INT NOT NULL COMMENT '代码行数',
    review_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审查时间',
    status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待处理/COMPLETED已完成/FAILED审查失败',
    task_id VARCHAR(100) DEFAULT NULL COMMENT '异步任务ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_review_time (review_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查记录表';

-- ============================================
-- 3. 审查问题表
-- ============================================
DROP TABLE IF EXISTS issue;
CREATE TABLE issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '问题ID',
    review_id BIGINT NOT NULL COMMENT '所属审查记录',
    rule_id BIGINT DEFAULT NULL COMMENT '触发的规则ID',
    severity ENUM('CRITICAL', 'ERROR', 'WARNING', 'SUGGESTION') NOT NULL COMMENT '严重级别：CRITICAL严重/ERROR错误/WARNING警告/SUGGESTION建议',
    line_number INT NOT NULL COMMENT '问题所在行号',
    description VARCHAR(500) NOT NULL COMMENT '问题描述',
    suggestion TEXT DEFAULT NULL COMMENT '修复建议',
    code_before TEXT DEFAULT NULL COMMENT '修改前代码片段',
    code_after TEXT DEFAULT NULL COMMENT '修改后代码示例',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_review_id (review_id),
    INDEX idx_severity (severity),
    INDEX idx_review_severity (review_id, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查问题表';

-- ============================================
-- 4. 审查规则表
-- ============================================
DROP TABLE IF EXISTS rule;
CREATE TABLE rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规则ID',
    category VARCHAR(50) NOT NULL COMMENT '分类：STYLE代码规范/DEFECT代码缺陷/SECURITY安全漏洞',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '规则名称',
    pattern_type VARCHAR(20) NOT NULL COMMENT '匹配模式类型：REGEX正则/AST语法树匹配',
    pattern VARCHAR(500) DEFAULT NULL COMMENT '具体匹配表达式',
    severity ENUM('CRITICAL', 'ERROR', 'WARNING', 'SUGGESTION') NOT NULL COMMENT '默认严重级别',
    class_id BIGINT DEFAULT NULL COMMENT '班级ID，为空代表全局公共规则',
    description VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
    suggestion_template TEXT DEFAULT NULL COMMENT '修复建议模板',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category),
    INDEX idx_enabled (enabled),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查规则表';

-- ============================================
-- 5. 班级表
-- ============================================
DROP TABLE IF EXISTS class;
CREATE TABLE class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
    teacher_id BIGINT NOT NULL COMMENT '管理教师ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ============================================
-- 6. 班级成员中间表
-- ============================================
DROP TABLE IF EXISTS class_user;
CREATE TABLE class_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_class_user (class_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级成员中间表';

-- ============================================
-- 7. 插入默认管理员账号
-- 密码: admin123 (BCrypt加密)
-- ============================================
INSERT INTO user (username, password, role, email) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 'admin@example.com'),
('teacher', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'TEACHER', 'teacher@example.com'),
('student', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'STUDENT', 'student@example.com');

-- ============================================
-- 8. 插入默认审查规则
-- ============================================
INSERT INTO rule (category, name, pattern_type, pattern, severity, description, suggestion_template, enabled) VALUES
-- 安全规则
('SECURITY', '检测硬编码密码', 'REGEX', '(password|passwd|pwd|secret)\\s*=\\s*[\"''][^\"'']{4,}[\"'']', 'CRITICAL', '检测到硬编码敏感信息', '将敏感信息移至配置文件或环境变量，使用System.getenv()或配置文件读取', 1),
('SECURITY', '检测SQL注入风险', 'REGEX', 'Statement\\s*\\.\\s*execute.*\\+', 'CRITICAL', '检测到SQL注入风险，使用Statement拼接SQL', '建议使用PreparedStatement预编译SQL，或使用MyBatis的#{}参数绑定', 1),
('SECURITY', '检测硬编码API Key', 'REGEX', '(api_key|apikey|access_key|accesskey)\\s*=\\s*[\"''][^\"'']+[\"'']', 'CRITICAL', '检测到硬编码API密钥', '将API密钥移至配置文件或环境变量，不要提交到代码仓库', 1),
('SECURITY', '检测硬编码令牌', 'REGEX', '(token|bearer)\\s*=\\s*[\"''][^\"'']{10,}[\"'']', 'CRITICAL', '检测到硬编码令牌', '将令牌移至安全存储，如环境变量或密钥管理服务', 1),

-- 缺陷规则
('DEFECT', '检测潜在空指针', 'REGEX', '\\.\\s*toString\\s*\\(\\s*\\)', 'WARNING', '方法返回值可能为null，调用toString()存在空指针风险', '在使用前添加 null 检查，如: if (obj != null) obj.toString()', 1),
('DEFECT', '检测未关闭资源', 'REGEX', 'new\\s+File(Input|Output)Stream', 'WARNING', '资源可能未正确关闭，存在资源泄漏风险', '建议使用 try-with-resources 语句确保资源正确关闭', 1),
('DEFECT', '检测System.out使用', 'REGEX', 'System\\.out\\.print', 'SUGGESTION', '代码中使用了System.out进行输出', '建议使用日志框架（如Logback、SLF4J）进行日志输出', 1),
('DEFECT', '检测空catch块', 'REGEX', 'catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}', 'WARNING', '存在空的异常捕获块', '异常捕获块应包含异常处理逻辑或日志记录', 1),

-- 风格规则
('STYLE', '检测TODO注释', 'REGEX', '//\\s*TODO|//\\s*FIXME', 'SUGGESTION', '存在未完成的TODO注释', '请及时完成TODO标记的任务或移除不需要的注释', 1),
('STYLE', '检测过长方法警告', 'AST', NULL, 'WARNING', '方法行数超过50行', '考虑将长方法拆分为多个小方法，提高代码可读性和可维护性', 1),
('STYLE', '检测魔法数字', 'REGEX', '[=+\\-*/%<>!&|]{1,3}\\s*\\d{4,}', 'SUGGESTION', '代码中存在硬编码的数字常量', '建议将魔法数字定义为命名常量，提高代码可读性', 1),
('STYLE', '检测缺少Javadoc', 'REGEX', 'public\\s+(class|interface|enum)', 'SUGGESTION', '公共类/接口缺少Javadoc注释', '建议为公共类型添加Javadoc注释，说明其用途和用法', 1);

-- ============================================
-- 9. 创建测试班级
-- ============================================
INSERT INTO class (class_name, description, teacher_id) VALUES
('Java编程基础', 'Java编程基础课程班级', 2),
('软件工程实践', '软件工程实践课程班级', 2);

-- ============================================
-- 10. 添加测试班级成员
-- ============================================
INSERT INTO class_user (class_id, user_id) VALUES
(1, 3),
(2, 3);

-- ============================================
-- 11. 插入测试审查记录（用于演示）
-- ============================================
INSERT INTO review (user_id, code_content, file_name, line_count, status) VALUES
(3, 'public class HelloWorld {\n    public static void main(String[] args) {\n        String password = "123456";\n        System.out.println("Hello");\n    }\n}', 'HelloWorld.java', 6, 'COMPLETED'),
(3, 'public class Calculator {\n    public int add(int a, int b) {\n        return a + b;\n    }\n    \n    public void process() {\n        // TODO: implement this method\n    }\n}', 'Calculator.java', 9, 'COMPLETED');

-- ============================================
-- 12. 插入测试审查问题
-- ============================================
INSERT INTO issue (review_id, severity, line_number, description, suggestion, code_before) VALUES
(1, 'CRITICAL', 3, '检测到硬编码密码', '将密码移至配置文件或环境变量', 'String password = "123456"'),
(1, 'SUGGESTION', 4, '使用System.out输出', '建议使用日志框架进行输出', 'System.out.println'),
(2, 'SUGGESTION', 6, '存在未完成的TODO注释', '请及时完成TODO标记的任务', '// TODO: implement this method');

-- ============================================
-- 完成提示
-- ============================================
SELECT '数据库初始化完成！' AS message;

-- 查询验证
SELECT '用户表数据:' AS info;
SELECT id, username, role, email FROM user;

SELECT '规则表数据:' AS info;
SELECT id, name, category, severity, enabled FROM rule;

SELECT '班级表数据:' AS info;
SELECT id, class_name, teacher_id FROM class;
