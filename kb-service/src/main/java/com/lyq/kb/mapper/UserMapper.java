package com.lyq.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyq.kb.entity.User;

/**
 * 不用写任何方法：BaseMapper已提供insert/deleteById/updateById/
 * selectById/selectList/selectCount等。启动类的@MapperScan会扫描到这个包
 */
public interface UserMapper extends BaseMapper<User> {
}