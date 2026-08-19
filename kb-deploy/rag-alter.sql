-- RAG向量检索升级：文档向量块表（存量库执行一次即可）
-- 保存文档时异步重建；老数据可通过 管理端"重建索引"按钮 或 POST /doc/reindex 回填
CREATE TABLE IF NOT EXISTS t_doc_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_id BIGINT NOT NULL COMMENT '文档ID',
  seq INT NOT NULL COMMENT '块序号',
  content TEXT NOT NULL COMMENT '块文本约400字',
  embedding MEDIUMTEXT COMMENT '向量JSON数组，embedding不可用时为空',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_doc(doc_id)
) COMMENT='文档向量块：RAG检索用，保存文档时重建';
