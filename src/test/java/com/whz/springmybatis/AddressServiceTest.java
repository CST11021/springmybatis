package com.whz.springmybatis;

import com.whz.springmybatis.entity.Address;
import com.whz.springmybatis.entity.User;
import com.whz.springmybatis.service.AddressService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AddressServiceTest {
    private ClassPathXmlApplicationContext context;
    private AddressService addressService;

    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext("classpath:spring_config/applicationContext.xml");
        addressService = (AddressService)context.getBean("addressService");
    }

    @Test
    public void insertAddress() {
        Address addr = new Address();
        User u = new User();
        u.setUid(1L);
        addr.setAddress("beijing");
        addr.setPostCode("100000");
        //		addr.setAid(100L);
        addr.setUser(u);
        //		Map<String,Object> map = new HashMap<String,Object>();
        //		map.put("address", addr);
        //		map.put("uid", 1L);
        addressService.insertAddress(addr);
        System.out.println(addr.getAid());
    }
}









