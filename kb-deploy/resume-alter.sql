-- 存量t_resume升级：加公共字段+提交流转列（由apply-resume-schema.ps1调用）
ALTER TABLE t_resume
    ADD COLUMN name         VARCHAR(50) COMMENT '姓名，拍平自content_json.basics.name',
    ADD COLUMN phone        VARCHAR(30) COMMENT '电话，拍平自basics.phone',
    ADD COLUMN city         VARCHAR(50) COMMENT '城市，拍平自basics.city',
    ADD COLUMN education    VARCHAR(20) COMMENT '最高学历：博士/硕士/本科/大专/其他',
    ADD COLUMN work_years   INT COMMENT '工作年限，按工作经历最早开始年份估算',
    ADD COLUMN skills       VARCHAR(500) COMMENT '技能摘要，分类:项目 顿号拼接',
    ADD COLUMN ai_score     INT COMMENT '最近一次AI分析得分',
    ADD COLUMN submit_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未提交 1待审阅 2已驳回 3已推荐',
    ADD COLUMN submit_time  DATETIME COMMENT '最近提交时间',
    ADD COLUMN applied_job_id BIGINT COMMENT '提交时意向岗位(t_job.id)，可空',
    ADD COLUMN assigned_job_id BIGINT COMMENT '管理员推荐岗位(t_job.id)，可空',
    ADD COLUMN remark       VARCHAR(255) COMMENT '管理员退回理由等备注',
    ADD KEY idx_submit (submit_status),
    ADD KEY idx_assigned (assigned_job_id);

-- 简历推荐岗位关系：一份简历可被推荐多个岗位，支持追加
CREATE TABLE IF NOT EXISTS t_resume_job (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id   BIGINT NOT NULL COMMENT '简历ID',
    job_id      BIGINT NOT NULL COMMENT '岗位ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    UNIQUE KEY uk_resume_job (resume_id, job_id),
    KEY idx_job (job_id)
) COMMENT='简历推荐岗位关系：一份简历可被推荐多个岗位，支持追加';
