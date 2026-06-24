# 第4章 数据库设计

## 4.1 ER图（实体关系图）

### 实体关系描述

本系统数据库设计包含以下核心实体及其关系：

```
┌─────────────┐         ┌─────────────────────┐         ┌─────────────┐
│    User     │────────▶│    ReviewTask       │◀────────│    Class    │
│  (用户)     │  1 : N  │  (代码审查任务)     │  1 : N  │  (班级)     │
└─────────────┘         └─────────────────────┘         └─────────────┘
       │                         │ 1 : N                        │
       │  N : M                  │                              │
       │   (class_user)          ▼                              │
       ▼                   ┌─────────────┐                      │
┌─────────────┐           │   Issue     │                      │
│    Class    │           │  (代码缺陷)  │                      │
└─────────────┘           └─────────────┘                      │
       │                                                   │
       └───────────────────────────────────────────────────┘
                         (班级成员可提交审查任务)
```

### 实体关系详解

| 关系 | 源实体 | 目标实体 | 关系类型 | 说明 |
|------|--------|----------|----------|------|
| 提交关系 | User | ReviewTask | 1 : N | 一个用户可创建多个审查任务 |
| 审核关系 | User (教师) | ReviewTask | 1 : N | 教师可审核多个审查任务 |
| 班级成员 | User | Class | N : M | 用户通过class_user中间表多对多关联 |
| 包含关系 | Class | ReviewTask | 1 : N | 一个班级可发起多个审查任务 |
| 包含缺陷 | ReviewTask | Issue | 1 : N | 一个任务可发现多个代码缺陷 |
| 分配关系 | User | Issue | 1 : N | 用户可被分配多个缺陷修复任务 |

### 实体属性概述

| 实体 | 主键 | 主要属性 |
|------|------|----------|
| User | id | username, email, password_hash, role, class_id |
| ReviewTask | id | title, status, submitter_id, reviewer_id, class_id, rule_set |
| Issue | id | task_id, severity, category, file_path, line_number, status |
| Rule | id | name, category, severity, pattern, message, enabled |
| Class | id | name, teacher_id, description |
| ClassUser | (class_id, user_id) | role_in_class, joined_at |

### 外键约束关系表（Foreign Key Relationships）

本系统各表之间的外键引用关系如下：

| 序号 | 源表 | 源字段 | 目标表 | 目标字段 | 说明 |
|------|------|--------|--------|----------|------|
| 1 | user | class_id | class | id | 用户所属班级 |
| 2 | review_task | submitter_id | user | id | 任务提交人 |
| 3 | review_task | reviewer_id | user | id | 任务审核人（教师） |
| 4 | review_task | class_id | class | id | 任务所属班级 |
| 5 | issue | task_id | review_task | id | 缺陷关联的任务 |
| 6 | issue | assignee_id | user | id | 缺陷分配给的用户 |
| 7 | class | teacher_id | user | id | 班级的班主任 |
| 8 | class_user | class_id | class | id | 班级成员关联的班级 |
| 9 | class_user | user_id | user | id | 班级成员关联的用户 |

---

## 4.2 user表（用户表）

### 建表SQL

```sql
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `role` TINYINT NOT NULL DEFAULT 3 COMMENT '角色:1=超管,2=教师,3=学生',
  `class_id` BIGINT COMMENT '所属班级ID',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除:0=未删除,1=已删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT `fk_user_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| id | BIGINT | N | 自增 | 用户ID，主键 |
| username | VARCHAR(64) | N | - | 用户名，唯一约束 |
| email | VARCHAR(255) | N | - | 邮箱，唯一约束 |
| password_hash | VARCHAR(255) | N | - | 密码哈希值 |
| role | TINYINT | N | 3 | 角色：1=超管，2=教师，3=学生 |
| class_id | BIGINT | Y | - | 所属班级ID，外键关联class表 |
| is_deleted | TINYINT | N | 0 | 逻辑删除标记 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | 最后更新时间 |

### 外键约束

| 约束名 | 字段 | 引用表 | 引用字段 | 删除操作 | 更新操作 |
|--------|------|--------|----------|----------|----------|
| fk_user_class | class_id | class | id | SET NULL | CASCADE |

---

## 4.3 review_task表（代码审查任务表）

### 建表SQL

```sql
CREATE TABLE `review_task` (
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
  CONSTRAINT `fk_review_task_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `user`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_review_task_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_review_task_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码审查任务表';
```

### 字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| id | BIGINT | N | 自增 | 任务ID，主键 |
| title | VARCHAR(255) | N | - | 任务标题 |
| status | TINYINT | N | 0 | 任务状态：0=创建，1=扫描中，2=完成，3=失败 |

### 任务状态整数值映射表

| 整数值 | 枚举值 | 中文含义 | 说明 |
|--------|--------|----------|------|
| 0 | PENDING | 已创建 | 任务刚创建，尚未开始扫描 |
| 1 | IN_PROGRESS | 扫描中 | 代码审查引擎正在执行扫描 |
| 2 | COMPLETED | 完成 | 扫描成功完成 |
| 3 | FAILED | 失败 | 扫描过程中发生错误 |
| submitter_id | BIGINT | N | - | 提交人ID，外键关联user表 |
| reviewer_id | BIGINT | Y | - | 审核人ID（教师），外键关联user表 |
| class_id | BIGINT | N | - | 所属班级ID，外键关联class表 |
| rule_set | VARCHAR(255) | N | 'default' | 启用的规则集名称 |
| source_path | VARCHAR(500) | Y | - | 源码路径 |
| result_summary | JSON | Y | - | 扫描结果摘要（JSON格式） |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | 最后更新时间 |

### 外键约束

| 约束名 | 字段 | 引用表 | 引用字段 | 删除操作 | 更新操作 |
|--------|------|--------|----------|----------|----------|
| fk_review_task_submitter | submitter_id | user | id | RESTRICT | CASCADE |
| fk_review_task_reviewer | reviewer_id | user | id | SET NULL | CASCADE |
| fk_review_task_class | class_id | class | id | RESTRICT | CASCADE |

---

## 4.4 issue表（代码缺陷表）

### 建表SQL

```sql
CREATE TABLE `issue` (
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
  CONSTRAINT `fk_issue_task` FOREIGN KEY (`task_id`) REFERENCES `review_task`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_issue_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user`(`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码缺陷表';
```

### 字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| id | BIGINT | N | 自增 | 缺陷ID，主键 |
| task_id | BIGINT | N | - | 关联的任务ID，外键关联review_task表 |
| severity | TINYINT | N | - | 严重程度：1=严重，2=高，3=中，4=低 |
| category | VARCHAR(64) | N | - | 缺陷分类（如：安全漏洞、代码风格等） |
| file_path | VARCHAR(500) | N | - | 缺陷所在文件路径 |
| line_number | INT | Y | - | 缺陷所在行号 |
| description | TEXT | Y | - | 缺陷详细描述 |
| suggestion | TEXT | Y | - | 修复建议 |
| status | TINYINT | N | 0 | 缺陷状态：0=OPEN（未处理/开放），1=ASSIGNED（已分配），2=RESOLVED（已修复），3=CLOSED（已关闭） |
| assignee_id | BIGINT | Y | - | 分配给谁（用户ID），外键关联user表 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | 创建时间 |

### 外键约束

| 约束名 | 字段 | 引用表 | 引用字段 | 删除操作 | 更新操作 |
|--------|------|--------|----------|----------|----------|
| fk_issue_task | task_id | review_task | id | CASCADE | CASCADE |
| fk_issue_assignee | assignee_id | user | id | SET NULL | CASCADE |

---

## 4.5 rule表（规则表）

### 建表SQL

```sql
CREATE TABLE `rule` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
  `name` VARCHAR(128) NOT NULL UNIQUE COMMENT '规则名称',
  `category` TINYINT NOT NULL COMMENT '分类:1=风格,2=安全,3=性能,4=最佳实践,5=正确性',
  `severity` TINYINT NOT NULL COMMENT '对应严重程度',
  `pattern` TEXT COMMENT '匹配模式(JSON)',
  `message` VARCHAR(500) COMMENT '提示信息',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用:1=启用,0=禁用',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码审查规则表';
```

### 字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| id | BIGINT | N | 自增 | 规则ID，主键 |
| name | VARCHAR(128) | N | - | 规则名称，唯一约束 |
| category | TINYINT | N | - | 规则分类：1=风格，2=安全，3=性能，4=最佳实践，5=正确性 |
| severity | TINYINT | N | - | 对应的缺陷严重程度 |
| pattern | TEXT | Y | - | 匹配模式（JSON格式） |
| message | VARCHAR(500) | Y | - | 缺陷检测提示信息 |
| enabled | TINYINT | N | 1 | 是否启用：1=启用，0=禁用 |
| version | INT | N | 1 | 规则版本号 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | N | CURRENT_TIMESTAMP | 最后更新时间 |

### 外键约束

> rule表为独立规则配置表，不引用其他表，无外键约束。

---

## 4.6 class表和class_user表（班级及班级成员表）

### class表建表SQL

```sql
CREATE TABLE `class` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
  `name` VARCHAR(128) NOT NULL COMMENT '班级名称',
  `teacher_id` BIGINT NOT NULL COMMENT '班主任ID',
  `description` TEXT COMMENT '班级描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  CONSTRAINT `fk_class_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user`(`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';
```

### class_user表建表SQL

```sql
CREATE TABLE `class_user` (
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_in_class` TINYINT DEFAULT 3 COMMENT '班级内角色:2=教师,3=学生',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`class_id`, `user_id`),
  CONSTRAINT `fk_class_user_class` FOREIGN KEY (`class_id`) REFERENCES `class`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_class_user_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  KEY `idx_class_user_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级用户关联表';
```

### class表字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| id | BIGINT | N | 自增 | 班级ID，主键 |
| name | VARCHAR(128) | N | - | 班级名称 |
| teacher_id | BIGINT | N | - | 班主任ID，外键关联user表 |
| description | TEXT | Y | - | 班级描述 |
| created_at | DATETIME | N | CURRENT_TIMESTAMP | 创建时间 |

### class_user表字段说明

| 字段名 | 数据类型 | 允许空 | 默认值 | 说明 |
|--------|----------|--------|--------|------|
| class_id | BIGINT | N | - | 班级ID，复合主键组成部分，外键关联class表 |
| user_id | BIGINT | N | - | 用户ID，复合主键组成部分，外键关联user表 |
| role_in_class | TINYINT | N | 3 | 班级内角色：2=教师，3=学生 |
| joined_at | DATETIME | N | CURRENT_TIMESTAMP | 加入班级时间 |

### class表外键约束

| 约束名 | 字段 | 引用表 | 引用字段 | 删除操作 | 更新操作 |
|--------|------|--------|----------|----------|----------|
| fk_class_teacher | teacher_id | user | id | RESTRICT | CASCADE |

### class_user表外键约束

| 约束名 | 字段 | 引用表 | 引用字段 | 删除操作 | 更新操作 |
|--------|------|--------|----------|----------|----------|
| fk_class_user_class | class_id | class | id | CASCADE | CASCADE |
| fk_class_user_user | user_id | user | id | CASCADE | CASCADE |

### class_user表复合主键说明

class_user表采用**复合主键** `(class_id, user_id)`，确保：
- 每个用户在同一班级中只有一条记录
- 唯一标识班级与用户的关联关系
- 无需额外的自增主键

---

## 4.7 外键约束关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              外键约束关系拓扑图                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐                                                            │
│  │    User     │◀─────────────────────────────────────────┐                 │
│  │             │                                          │                 │
│  │  id (PK)    │◀──fk_user_class(class_id)                │                 │
│  └─────────────┘                                          │                 │
│       ▲                                                   │                 │
│       │                                                   │                 │
│       │ fk_class_teacher(teacher_id)                      │                 │
│       │ fk_review_task_submitter(submitter_id)            │                 │
│       │ fk_review_task_reviewer(reviewer_id)              │                 │
│       │ fk_issue_assignee(assignee_id)                    │                 │
│       │ fk_class_user_user(user_id)                       │                 │
│       │                                                   │                 │
│  ┌─────────────┐       ┌─────────────────────┐            │                 │
│  │    Class    │       │    ReviewTask       │            │                 │
│  │             │       │                     │            │                 │
│  │  id (PK)    │◀──────│ class_id (FK)       │            │                 │
│  │ teacher_id  │       │ submitter_id (FK)   │            │                 │
│  └─────────────┘       │ reviewer_id (FK)    │            │                 │
│       ▲                └─────────────────────┘            │                 │
│       │                       │                            │                 │
│       │ fk_class_user_class   │ fk_issue_task              │                 │
│       │                       ▼                            │                 │
│       │                ┌─────────────┐                     │                 │
│       │                │   Issue     │                     │                 │
│       │                │             │                     │                 │
│       │                │ task_id(FK) │                     │                 │
│       │                │assignee_id  │                     │                 │
│       │                └─────────────┘                     │                 │
│       │                                                      │                 │
│  ┌─────────────┐                                             │                 │
│  │ class_user  │                                             │                 │
│  │(复合主键)   │                                             │                 │
│  │class_id(PK) │                                             │                 │
│  │user_id (PK) │                                             │                 │
│  └─────────────┘                                             │                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4.8 索引设计

### 索引概述

合理的索引设计可以显著提升查询性能。以下为本系统的索引设计说明。

### user表索引

| 索引名称 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| PRIMARY | id | 主键索引 | 主键索引，唯一且非空 |
| idx_role | role | 普通索引 | 用于按角色筛选用户 |
| idx_class_id | class_id | 普通索引 | 用于按班级筛选用户 |

### review_task表索引

| 索引名称 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| PRIMARY | id | 主键索引 | 主键索引，唯一且非空 |
| idx_class_id | class_id | 普通索引 | 用于按班级查询任务列表 |
| idx_status | status | 普通索引 | 用于按状态筛选任务 |
| idx_submitter | submitter_id | 普通索引 | 用于查询某用户提交的任务 |

### issue表索引

| 索引名称 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| PRIMARY | id | 主键索引 | 主键索引，唯一且非空 |
| idx_task_id | task_id | 普通索引 | 用于查询某任务的所有缺陷 |
| idx_status | status | 普通索引 | 用于按状态筛选缺陷 |
| idx_assignee | assignee_id | 普通索引 | 用于查询分配给某用户的缺陷 |

### rule表索引

| 索引名称 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| PRIMARY | id | 主键索引 | 主键索引，唯一且非空 |
| idx_enabled | enabled | 普通索引 | 用于查询已启用的规则 |
| idx_category | category | 普通索引 | 用于按分类查询规则 |

### class_user表索引

| 索引名称 | 字段 | 类型 | 说明 |
|----------|------|------|------|
| PRIMARY | (class_id, user_id) | 复合主键索引 | 复合主键，唯一且非空 |
| idx_class_user_user_id | user_id | 普通索引 | 用于查询某用户加入的所有班级 |

---

## 4.9 索引SQL汇总

```sql
-- user表索引
CREATE INDEX idx_role ON `user` (`role`);
CREATE INDEX idx_class_id ON `user` (`class_id`);

-- review_task表索引
CREATE INDEX idx_class_id ON review_task (`class_id`);
CREATE INDEX idx_status ON review_task (`status`);
CREATE INDEX idx_submitter ON review_task (`submitter_id`);

-- issue表索引
CREATE INDEX idx_task_id ON issue (`task_id`);
CREATE INDEX idx_status ON issue (`status`);
CREATE INDEX idx_assignee ON issue (`assignee_id`);

-- rule表索引
CREATE INDEX idx_enabled ON rule (`enabled`);
CREATE INDEX idx_category ON rule (`category`);
```

---

## 4.10 数据库完整性约束汇总

### 外键约束一览表

| 约束名称 | 源表 | 源字段 | 目标表 | 目标字段 | 级联删除 | 级联更新 |
|----------|------|--------|--------|----------|----------|----------|
| fk_user_class | user | class_id | class | id | SET NULL | CASCADE |
| fk_review_task_submitter | review_task | submitter_id | user | id | RESTRICT | CASCADE |
| fk_review_task_reviewer | review_task | reviewer_id | user | id | SET NULL | CASCADE |
| fk_review_task_class | review_task | class_id | class | id | RESTRICT | CASCADE |
| fk_issue_task | issue | task_id | review_task | id | CASCADE | CASCADE |
| fk_issue_assignee | issue | assignee_id | user | id | SET NULL | CASCADE |
| fk_class_teacher | class | teacher_id | user | id | RESTRICT | CASCADE |
| fk_class_user_class | class_user | class_id | class | id | CASCADE | CASCADE |
| fk_class_user_user | class_user | user_id | user | id | CASCADE | CASCADE |

### 约束行为说明

| 操作类型 | RESTRICT | CASCADE | SET NULL | NO ACTION |
|----------|----------|---------|----------|-----------|
| 删除父记录 | 拒绝删除 | 级联删除子记录 | 子记录外键设为NULL | 拒绝删除 |
| 更新父键值 | 拒绝更新 | 级联更新子外键 | 子记录外键设为NULL | 拒绝更新 |

### 复合主键定义

| 表名 | 主键字段 | 主键类型 | 说明 |
|------|----------|----------|------|
| class_user | (class_id, user_id) | 复合主键 | 确保班级-用户组合唯一 |