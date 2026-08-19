-- 面试系统改造：支持简历驱动面试 + 着装评估
ALTER TABLE t_interview
  ADD COLUMN resume_id bigint DEFAULT NULL COMMENT '关联简历ID' AFTER category,
  ADD COLUMN resume_review longtext COMMENT 'JSON简历审核结果' AFTER score,
  ADD COLUMN appearance_score int DEFAULT NULL COMMENT '着装评分0-100' AFTER resume_review,
  ADD COLUMN appearance_review longtext COMMENT 'JSON着装评估结果' AFTER appearance_score;
