package com.lyq.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyq.kb.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT DISTINCT category FROM t_question WHERE deleted = 0 ORDER BY category")
    List<String> selectCategories();
}
