package com.whz.springmybatis.dao;

import java.util.List;
import java.util.Map;

import com.whz.springmybatis.entity.User;
//import org.mybatis.spring.annotation.Mapper;
//
//@Mapper
public interface UserDao {
    void insertUser(User user);

    void updateUser(User user);

    void deleteUser(Long uid);

    /**
     * 进行模糊查询
     *
     * @param likeCondition
     * @return
     */
    List<User> getAllUser(Map<String, Object> likeCondition);

    User getUser(Long uid);
}
