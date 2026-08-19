-- 岗位表：爬虫抓取 + 管理员手动录入，统一走审核流
CREATE DATABASE IF NOT EXISTS kb DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE kb;

CREATE TABLE IF NOT EXISTS `t_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source` varchar(20) NOT NULL DEFAULT 'BOSS' COMMENT '数据来源：BOSS=Boss直聘爬取 MANUAL=管理员手动录入',
  `source_id` varchar(64) DEFAULT NULL COMMENT '来源站唯一标识（Boss用job_detail的securityId或URL的MD5），去重用',
  `title` varchar(100) NOT NULL COMMENT '岗位名',
  `company` varchar(100) DEFAULT NULL COMMENT '公司名',
  `city` varchar(50) DEFAULT NULL COMMENT '工作城市',
  `salary` varchar(50) DEFAULT NULL COMMENT '薪资原文，如 15-25K·14薪',
  `experience` varchar(50) DEFAULT NULL COMMENT '经验要求原文，如 3-5年',
  `education` varchar(50) DEFAULT NULL COMMENT '学历要求原文，如 本科',
  `skills_json` text COMMENT '技能标签JSON数组，来源站标签或AI解析结果',
  `jd_text` mediumtext COMMENT 'JD职责描述原文',
  `require_json` text COMMENT 'AI结构化需求：{skills:[],minExpYears,education,keywords:[]}',
  `job_url` varchar(300) DEFAULT NULL COMMENT '岗位详情原始链接',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审核 1已上架 2已驳回/下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取/录入时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source`,`source_id`),
  KEY `idx_status` (`status`),
  KEY `idx_city` (`city`),
  FULLTEXT KEY `ft_jd` (`title`,`jd_text`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位表（爬虫+手动录入）';
