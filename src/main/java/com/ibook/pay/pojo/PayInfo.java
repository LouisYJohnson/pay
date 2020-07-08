package com.ibook.pay.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

//使用lombok,lombok解决了get,set方法,但是构造方法需要自己写
@Data
public class PayInfo {
    private Integer id;

    private Integer userId;

    private Long orderNo;

    private Integer payPlatform;

    private String platformNumber;

    private String platformStatus;

    private BigDecimal payAmount;

    private Date createTime;

    private Date updateTime;

    //id是自增的,不需要传入,userId在这个业务中不关心,创建时间和更新时间数据库自动更新
    public PayInfo(Long orderNo, Integer payPlatform, String platformStatus, BigDecimal payAmount) {
        this.orderNo = orderNo;
        this.payPlatform = payPlatform;
        this.platformStatus = platformStatus;
        this.payAmount = payAmount;
    }
}