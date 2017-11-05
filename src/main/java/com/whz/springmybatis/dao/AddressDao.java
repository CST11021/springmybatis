package com.whz.springmybatis.dao;

import java.util.List;

import com.whz.springmybatis.entity.Address;
import com.whz.springmybatis.entity.Pagination;
//import org.mybatis.spring.annotation.Mapper;
//
//@Mapper
public interface AddressDao {
    Integer insertAddress(Address addr);

    void updateAddress(Address addr);

    void deleteAddress(Long aid);

    List<Address> allAddress(Pagination page);

    Address getAddress(Long aid);
}
