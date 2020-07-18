package com.ibook.pay.service.impl;

import com.google.gson.Gson;
import com.ibook.pay.dao.PayInfoMapper;
import com.ibook.pay.enums.PayPlatformEnum;
import com.ibook.pay.pojo.PayInfo;
import com.ibook.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    private static final String QUEUE_PAY_NOTIFY = "payNotify";

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    //这里标红是idea识别问题,没有影响
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    //因为有两种支付方式,所以传入支付的枚举类,并在方法中做判断,不同的支付方式对应不同的处理方法
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {
//        //加入判断(去掉,因为在controller中判断了)
//        //在网页传入参数的时候,枚举类型写后面的,比如微信支付参数写为WXPAY_NATIVE
//        //用枚举,大小写区分,必须一模一样才行
//        if (bestPayTypeEnum != BestPayTypeEnum.WXPAY_NATIVE
//        && bestPayTypeEnum != BestPayTypeEnum.ALIPAY_PC) {
//            throw new RuntimeException("暂不支持的支付类型");
//        }

        //写入数据库(创建订单的时候应该将订单数据写入数据库)
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),//构造一个枚举类断传入的支付方式并转成1,2
                OrderStatusEnum.NOTPAY.name(),
                amount);
        //将创建的订单pojo写入数据库
        //如果使用insert方法,好多字段在pojo中都没有传
        //所以使用insertSelective来选择性的传入写了的参数
        payInfoMapper.insertSelective(payInfo);


        PayRequest request = new PayRequest();
        request.setOrderName("9120970-12456");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(bestPayTypeEnum);


        PayResponse response = bestPayService.pay(request);
        log.info("response={}", response);
        return response;
    }

    /**
     * 异步通知处理
     *
     * @param notifyData
     */
    @Override   //只是方法重名,bestPayService本身自带asyncNotify方法
    public String asyncNotify(String notifyData) {
        //1.签名校验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("notifyData={}", notifyData);

        //2.金额校验(从数据库中查订单)数据库中的金额与异步通知中的金额做校验(应该通过订单号查询)
        //比较严重(正常情况下不会发生的) 发出告警:钉钉,短信告警
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if (payInfo == null) {  //如果回来的单号在数据库中并没有,说明有人动过数据库了,一般情况下是不可能出现的
            //发出告警:钉钉,短信告警,查看这种严重情况是咋回事
            throw new RuntimeException("通过orderNo查询到的结果是null");
        }
        //如果此时订单在系统中还没有支付而在微信那边已经支付了,此时来比较金额对不对
        if (!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())) {
            //Double 类型比较大小,精度问题不好控制
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
                //compareTo结果为0就是等于,看源码里面有注释
                //如果金额还不相同,就告警
                //要写详细
                throw new RuntimeException("异步通知中的金额与数据库里的不一致,orderNo=" + payResponse.getOrderId());
            }
            //TODO pay发送MQ消息,mall接受MQ消息(mall还没写好,后面搞)
            //最好传一个json到消息队列中,如果传对象在rabbitmq上看不见对象的信息
            //因为在实际的生产环境中也是要到可视化管理界面去看到底哪里有问题,所以还是变成json更好
            amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY, new Gson().toJson(payInfo));

            //到这里,说明没支付且订单金额相同
            //3.如果通过,修改订单的支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            //写入交易流水号(由支付平台相应,是一个交易的票据)
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            //因为pojo从数据库中查出来的时候就是带着时间的,这个时间又被set进去了,
            //这个拿出来的时间就是初始的时间,也就是和create_time一致的时间,所以会造成写入后create_time与update_time相等
            //所以这里将其设置为null,让数据库自己改值
            //或者在xml的sql语句中将这个变量删了
            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        //4.告诉平台不要再通知了(同一个订单平台会多次向本机发订单的通知)
        //此处需要引入判断
        //不同的支付平台,异步通知返回给微信或者支付宝的东西是不一样的
        //微信是xml,支付宝是字符串"success"
        if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX) {
            return "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        } else if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY) {
            return "success";
        }

        throw new RuntimeException("异步通知中错误的支付平台");
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}
