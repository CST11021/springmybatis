package com.whz.springmybatis.service.impl;

import com.whz.springmybatis.dao.AddressDao;
import com.whz.springmybatis.entity.Address;
import com.whz.springmybatis.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("addressService")
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressDao addressDao;

    @Override
    public void insertAddress(Address addr) {
        Integer aid = addressDao.insertAddress(addr);
    }

    public void setAddressDao(AddressDao addressDao) {
        this.addressDao = addressDao;
    }
}
