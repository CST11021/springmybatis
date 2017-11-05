package com.whz.springmybatis.service.impl;

import java.util.List;
import java.util.Map;

import com.whz.springmybatis.dao.AddressDao;
import com.whz.springmybatis.dao.UserDao;
import com.whz.springmybatis.entity.Address;
import com.whz.springmybatis.entity.User;
import com.whz.springmybatis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private AddressDao addressDao;

    @Override
    public void deleteUser(Long uid) {
        userDao.deleteUser(uid);
    }

    /**
     * 查询出全部的User。
     *
     * @param page      分页页码。
     * @param pageCount 页数。
     */
    @Override
    public List<User> getAllUser(Integer page, Integer pageCount) {
        List<User> users = userDao.getAllUser(null);
        return users;
    }

    @Override
    public User getOneUser(Long uid) {
        User user = userDao.getUser(uid);
        return user;
    }

    /**
     * 通配查找需要的数据。
     */
    @Override
    public List<User> getUserNeeded(Map<String, Object> likeCondition) {
        List<User> users = userDao.getAllUser(likeCondition);
        return users;
    }

    /**
     * 级联插入Address
     */
    @Override
    public User insertUser(User user) {
        userDao.insertUser(user);
        List<Address> addrs = user.getAddrs();
        //		int a = 9 / 0;
        if (addrs != null && addrs.size() > 0) {
            for (Address addr : addrs) {
                addr.setUser(user);
                addressDao.insertAddress(addr);
            }
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        userDao.updateUser(user);
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setAddressDao(AddressDao addressDao) {
        this.addressDao = addressDao;
    }

}
