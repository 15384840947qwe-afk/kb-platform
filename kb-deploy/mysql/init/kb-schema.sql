
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `kb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `kb`;
DROP TABLE IF EXISTS `t_catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id` bigint NOT NULL COMMENT '所属知识库',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父节点id，0=根节点，自关联成树',
  `title` varchar(100) NOT NULL COMMENT '节点名（文件夹名或文档标题）',
  `node_type` varchar(10) NOT NULL COMMENT 'FOLDER=文件夹 DOC=文档',
  `doc_id` bigint DEFAULT NULL COMMENT 'node_type=DOC时指向t_doc实体',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同级排序值，小的在前（拖拽排序就改它）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0待审核 1通过 2驳回',
  `create_by` bigint DEFAULT NULL COMMENT '创建人id',
  PRIMARY KEY (`id`),
  KEY `idx_kb_parent` (`kb_id`,`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7661 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目录树表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_doc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id` bigint NOT NULL COMMENT '所属知识库：冗余一份，全库搜索/统计不用绕目录树',
  `title` varchar(200) NOT NULL COMMENT '文档标题',
  `content` longtext COMMENT 'Editor.js JSON内容。TEXT上限64KB长文不够，用LONGTEXT',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号：保存时version匹配才更新，防两人同改互踩',
  `creator_id` bigint NOT NULL COMMENT '创建人',
  `updater_id` bigint DEFAULT NULL COMMENT '最后修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0待审核 1通过 2驳回',
  `parent_id` bigint DEFAULT NULL COMMENT '提交时想挂的文件夹，审核通过时才真正挂上去',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6604 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id` bigint NOT NULL COMMENT '所属知识库',
  `doc_id` bigint DEFAULT NULL COMMENT '在哪个文档里上传的；传在知识库根目录则为NULL',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名，展示和下载用',
  `object_key` varchar(255) NOT NULL COMMENT 'MinIO对象键，二进制唯一存放处',
  `size` bigint NOT NULL COMMENT '文件字节数',
  `content_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型，决定浏览器能否直接预览',
  `uploader_id` bigint NOT NULL COMMENT '上传人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_kb` (`kb_id`),
  KEY `idx_doc` (`doc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件元数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '哪个用户',
  `doc_id` bigint NOT NULL COMMENT '看了哪篇',
  `kb_id` bigint NOT NULL COMMENT '冗余知识库id，跳转时不用反查',
  `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次查看',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_doc` (`user_id`,`doc_id`),
  KEY `idx_user_time` (`user_id`,`view_time`)
) ENGINE=InnoDB AUTO_INCREMENT=14579 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浏览历史';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '谁面的',
  `category` varchar(50) NOT NULL COMMENT '科目',
  `score` int DEFAULT NULL COMMENT '总评分0-100',
  `transcript` longtext COMMENT 'JSON全文对话',
  `report` longtext COMMENT 'JSON总评',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT NULL COMMENT '简介，首页卡片展示用',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图URL（存MinIO）',
  `owner_id` bigint NOT NULL COMMENT '创建人用户id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0待审核 1通过 2驳回',
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_practice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_practice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '谁刷的',
  `question_id` bigint NOT NULL COMMENT '哪道题',
  `result` tinyint NOT NULL COMMENT '1对/会 0错/不会',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`create_time`),
  KEY `idx_user_question` (`user_id`,`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category` varchar(50) NOT NULL COMMENT '科目：Java基础/MySQL/Redis等',
  `type` varchar(10) NOT NULL COMMENT 'SINGLE单选 MULTI多选 FILL填空 SHORT简答',
  `stem` text NOT NULL COMMENT '题干',
  `options` text COMMENT '选项JSON数组，选择题才有',
  `answer` text NOT NULL COMMENT '正确答案：单选B 多选AC 填空文本 简答参考答案',
  `explanation` text COMMENT '解析',
  `related_doc_id` bigint DEFAULT NULL COMMENT '关联教材文档id，可空',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='固定题库';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_resume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_resume` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户',
  `title` varchar(100) NOT NULL COMMENT '简历标题，默认取导入文件名',
  `target_job` varchar(100) DEFAULT NULL COMMENT '目标岗位，如Java后端开发/新媒体运营/高中教师',
  `raw_text` text COMMENT '导入原文（PDF抽出的纯文本）或生成的Markdown全文',
  `content_json` mediumtext COMMENT '结构化简历JSON',
  `analysis_json` text COMMENT '最近一次AI分析结果JSON',
  `file_name` varchar(255) DEFAULT NULL COMMENT '导入的原始文件名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='简历';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名，唯一',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt哈希固定60字符，留余量',
  `nickname` varchar(50) DEFAULT NULL COMMENT '显示昵称，可与登录名不同',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像，存MinIO访问URL',
  `role` varchar(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：ADMIN/MEMBER/VIEWER',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0禁用：踢人不用删数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0正常 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

