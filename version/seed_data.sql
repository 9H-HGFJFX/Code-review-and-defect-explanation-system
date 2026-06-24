USE code_review;
INSERT INTO class (name, teacher_id, description) VALUES
  ('Software Engineering 2024-1', 1, 'Student code review class'),
  ('Software Engineering 2024-2', 1, 'Second semester class');

INSERT INTO review_rule (rule_id, name, description, category, severity, languages, pattern, enabled, priority, version, creator_id) VALUES
  ('R001', 'R001-NoHardcodedPwd', 'Detected hardcoded password', 2, 1, 'java,python', 'password\\s*=\\s*["][^"]+["]', 1, 100, '1.0', 1),
  ('R002', 'R002-NoSystemOut', 'Avoid System.out in production', 3, 2, 'java', 'System\\.out\\.print', 1, 90, '1.0', 1),
  ('R003', 'R003-NoTabs', 'Use spaces instead of tabs', 1, 3, 'java,python', '\\t', 1, 80, '1.0', 1),
  ('R004', 'R004-LongMethods', 'Methods over 50 lines', 4, 2, 'java', 'TOO_LONG', 1, 70, '1.0', 1),
  ('R005', 'R005-NoConsoleLog', 'Avoid console.log', 3, 2, 'javascript,typescript', 'console\\.log', 1, 90, '1.0', 1);

INSERT INTO review_task (title, description, class_id, submitter_id, status, deadline) VALUES
  ('Homework 1 - Java Code Review', 'Scan student Java code for security and style', 1, 1, 0, '2026-07-01 23:59:59'),
  ('Midterm Project', 'Full project scan', 1, 1, 1, '2026-07-15 23:59:59'),
  ('Final Project', 'Comprehensive final review', 2, 1, 2, '2026-08-01 23:59:59');

SELECT 'Classes' AS section, COUNT(*) AS cnt FROM class
UNION SELECT 'Rules', COUNT(*) FROM review_rule
UNION SELECT 'Tasks', COUNT(*) FROM review_task;