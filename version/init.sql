-- 代码审查系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS code_review DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE code_review;

-- =============================================
-- 准备：删除所有表（按依赖顺序反向删除）
-- =============================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `issue`;
DROP TABLE IF EXISTS `class_user`;
DROP TABLE IF EXISTS `review_task`;
DROP TABLE IF EXISTS `rule`;
DROP TABLE IF EXISTS `class`;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 第一步：创建无外键的基础表
-- =============================================

-- 用户表（暂时不添加 class_id 的外键约束）
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `role` TINYINT NOT NULL DEFAULT 3 COMMENT '角色:1=超管,2=教师,3=学生',
  `class_id` BIGINT COMMENT '所属班级ID',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除:0=未删除,1=已删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_role` (`role`),
  INDEX `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 班级表（暂时不添加 teacher_id 的外键约束）
CREATE TABLE IF NOT EXISTS `class` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
  `name` VARCHAR(128) NOT NULL COMMENT '班级名称',
  `teacher_id` BIGINT NOT NULL COMMENT '班主任ID',
  `description` TEXT COMMENT '班级描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 规则表（无外键依赖）
CREATE TABLE IF NOT EXISTS `rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
  `name` VARCHAR(128) NOT NULL UNIQUE COMMENT '规则名称',
  `category` TINYINT NOT NULL COMMENT '分类:1=风格,2=安全,3=性能,4=最佳实践,5=正确性',
  `severity` TINYINT NOT NULL COMMENT '对应严重程度',
  `pattern` TEXT COMMENT '匹配模式(JSON)',
  `message` VARCHAR(500) COMMENT '提示信息',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用:1=启用,0=禁用',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_enabled` (`enabled`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码审查规则表';

-- 审查任务表（暂时不添加外键约束）
CREATE TABLE IF NOT EXISTS `review_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  `title` VARCHAR(255) NOT NULL COMMENT '任务标题',
  `status` TINYINT DEFAULT 0 COMMENT '状态:0=创建,1=扫描中,2=完成,3=失败',
  `submitter_id` BIGINT NOT NULL COMMENT '提交人ID',
  `reviewer_id` BIGINT COMMENT '审核人ID(教师)',
  `class_id` BIGINT NOT NULL COMMENT '所属班级ID',
  `rule_set` VARCHAR(255) DEFAULT 'default' COMMENT '启用的规则集',
  `source_path` VARCHAR(500) COMMENT '源码路径',
  `result_summary` JSON COMMENT '扫描结果摘要',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_class_id` (`class_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_submitter` (`submitter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码审查任务表';

-- 缺陷表（暂时不添加外键约束）
CREATE TABLE IF NOT EXISTS `issue` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缺陷ID',
  `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
  `severity` TINYINT NOT NULL COMMENT '严重程度:1=严重,2=高,3=中,4=低',
  `category` VARCHAR(64) NOT NULL COMMENT '缺陷分类',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `line_number` INT COMMENT '行号',
  `description` TEXT COMMENT '缺陷描述',
  `suggestion` TEXT COMMENT '修复建议',
  `status` TINYINT DEFAULT 0 COMMENT '状态:0=未处理,1=已分配,2=已修复,3=已关闭',
  `assignee_id` BIGINT COMMENT '分配给谁',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_task_id` (`task_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码缺陷表';

-- 班级用户关联表（暂时不添加外键约束）
CREATE TABLE IF NOT EXISTS `class_user` (
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_in_class` TINYINT DEFAULT 3 COMMENT '班级内角色:2=教师,3=学生',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`class_id`, `user_id`),
  INDEX `idx_class_user_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级用户关联表';

-- =============================================
-- 第二步：添加所有外键约束
-- =============================================

-- user表的class_id引用class表
ALTER TABLE `user` ADD CONSTRAINT `fk_user_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- class表的teacher_id引用user表
ALTER TABLE `class` ADD CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- review_task表的外键
ALTER TABLE `review_task` ADD CONSTRAINT `fk_review_task_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `review_task` ADD CONSTRAINT `fk_review_task_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `review_task` ADD CONSTRAINT `fk_review_task_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

-- issue表的外键
ALTER TABLE `issue` ADD CONSTRAINT `fk_issue_task` FOREIGN KEY (`task_id`) REFERENCES `review_task`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `issue` ADD CONSTRAINT `fk_issue_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user`(`id`) ON DELETE SET NULL ON UPDATE CASCADE;

-- class_user表的外键
ALTER TABLE `class_user` ADD CONSTRAINT `fk_class_user_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `class_user` ADD CONSTRAINT `fk_class_user_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 第三步：初始化数据
-- =============================================

-- 初始化管理员用户（密码: admin123）
-- 密码哈希使用BCrypt，实际生产环境请使用更强壮的密码
INSERT INTO `user` (`username`, `email`, `password_hash`, `role`, `class_id`) VALUES
('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, NULL);

-- 初始化默认规则
INSERT INTO `rule` (`name`, `category`, `severity`, `pattern`, `message`, `enabled`) VALUES
-- 安全规则
('R001', 2, 1, '{"type":"regex","pattern":"password\\s*=\\s*[\"\''][^\"\'']+[\"\'']"}', '检测到硬编码密码', 1),
('R002', 2, 1, '{"type":"regex","pattern":"api[_-]?key\\s*=\\s*[\"\''][^\"\'']+[\"\'']"}', '检测到硬编码API密钥', 1),
('R003', 2, 2, '{"type":"regex","pattern":"SQL.*SELECT.*FROM.*WHERE.*\\+"}', '可能的SQL注入风险', 1),
-- 风格规则
('R004', 1, 3, '{"type":"regex","pattern":"\\t"}', '使用空格代替Tab缩进', 1),
('R005', 1, 4, '{"type":"regex","pattern":"\\r\\n"}', '使用Unix风格换行符', 1),
-- 性能规则
('R006', 3, 2, '{"type":"regex","pattern":"System\\.out\\.println"}', '避免在生产代码中使用System.out', 1),
('R007', 3, 3, '{"type":"regex","pattern":"String\\s+\\w+\\s*=\\s*new\\s+String\\("}', '避免不必要的String对象创建', 1),
-- 最佳实践
('R008', 4, 2, '{"type":"regex","pattern":"catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)"}', '避免捕获通用异常', 1),
('R009', 4, 3, '{"type":"regex","pattern":"public\\s+void\\s+\\w+\\s*\\([^)]*\\)"}', '方法参数过多，考虑重构', 1),
-- 正确性
('R010', 5, 1, '{"type":"regex","pattern":"==\\s*null|null\\s*=="}', '使用Objects.equals()进行null比较', 1);