package com.ibook.pay.dao;

import com.ibook.pay.pojo.PayInfo;

public interface PayInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PayInfo record);

    int insertSelective(PayInfo record);

    PayInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(PayInfo record);

    int updateByPrimaryKey(PayInfo record);

    //这里新增一个方法,用订单号来查询订单
    PayInfo selectByOrderNo(Long orderNo);
}