package com.whz.springmybatis;

import com.whz.springmybatis.view.UserController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SingltonTest {
    private ClassPathXmlApplicationContext context;
    private UserController userController;

    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("spring-config/applicationContext-mvc.xml");
        userController = (UserController)context.getBean("UserController");
    }

    @Test
    public void test() {
        UserController controll1 = (UserController)context.getBean("UserController");
        UserController controll2 = (UserController)context.getBean("UserController");
        //System.out.println(controll1.getUserService() == controll2.getUserService());
    }
}
