package com.lyq.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyq.kb.entity.History;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 继承BaseMapper白拿delete/selectCount；
 * upsert记录查看靠唯一键uk_user_doc"再看只更新时间"
 */
public interface HistoryMapper extends BaseMapper<History> {

    /** 看过就插入；再看靠唯一键uk_user_doc触发"只更新时间" */
    @Insert("INSERT INTO t_history(user_id, doc_id, kb_id) " +
            "VALUES(#{userId}, #{docId}, #{kbId}) " +
            "ON DUPLICATE KEY UPDATE view_time = NOW()")
    int upsertView(@Param("userId") Long userId, @Param("docId") Long docId, @Param("kbId") Long kbId);

    /** 分页取某用户历史，按查看时间倒序 */
    @Select("SELECT * FROM t_history WHERE user_id = #{userId} " +
            "ORDER BY view_time DESC LIMIT #{size} OFFSET #{offset}")
    List<History> selectPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);
}
