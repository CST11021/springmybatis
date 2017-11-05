package com.whz.springmybatis;

import com.whz.springmybatis.dao.UserDao;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class UserDaoTest {
    private ClassPathXmlApplicationContext context;
    private UserDao userDao;

    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("spring_config/applicationContext.xml");
        userDao = (UserDao)context.getBean("userDao");
    }

    //INSERT INTO USER_INFO VALUES(1,'admin','123','1986-11-24',1,20);
    @Test
    public void testGetOneUser() {

        System.out.println(userDao.getUser(1l).getName() + "---");
    }

}
