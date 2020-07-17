package com.ibook.pay.controller;

import com.ibook.pay.pojo.PayInfo;
import com.ibook.pay.service.impl.PayServiceImpl;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.model.PayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/pay")
@Slf4j
public class PayController {

    @Autowired
    private PayServiceImpl payServiceImpl;
    @Autowired
    private WxPayConfig wxPayConfig;

    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId") String orderId,
                               @RequestParam("amount") BigDecimal amount,
                               @RequestParam("payType") BestPayTypeEnum bestPayTypeEnum
    ) {
        PayResponse payResponse = payServiceImpl.create(orderId, amount, bestPayTypeEnum);

        //支付方式不同,渲染就不同,微信native的codeUrl在支付宝的pc就不好使
        //微信native支付返回一个二维码的地址,codeUrl,然后我们把这个地址给转成二维码
        //支付宝pc使用body字段
        //所以我们这里加入判断,不同的支付方式对应不同的渲染方式
        //对应的,也要构建两个不同的网页对应支付
        Map<String, String> map = new HashMap();
        if (bestPayTypeEnum == BestPayTypeEnum.WXPAY_NATIVE) {
            map.put("codeUrl", payResponse.getCodeUrl());
            map.put("orderId", orderId);
            //配置性的东西,已经配置好了,直接注入到IOC中再@Autowired注入取属性即可
            map.put("returnUrl", wxPayConfig.getReturnUrl());
            return new ModelAndView("createForWxNative", map);
        } else if (bestPayTypeEnum == BestPayTypeEnum.ALIPAY_PC) {
            map.put("body", payResponse.getBody());
            return new ModelAndView("createForAliPayPc", map);
        }
        //如果到这里了,说明不是支持的支付方式,直接抛出异常即可
        throw new RuntimeException("暂不支持的支付类型");

    }

    @PostMapping("/notify")
    //@ResponseBody 注解的作用是将 Controller 的方法返回的对象通过适当的转换器转换为指定的格式之后，
    // 写入到HTTP 响应(Response)对象的 body 中，通常用来返回 JSON 或者 XML 数据，返回 JSON 数据的情况比较多。
    //这里是用来给微信返回xml格式的数据
    //一般都是@Controller与@ResponseBody一起用
    @ResponseBody
    public String asyncNotify(@RequestBody String notifyData) {
        return payServiceImpl.asyncNotify(notifyData);
    }

    //通过订单号查询支付状态的API
    @GetMapping("/queryByOrderId")
    @ResponseBody
    public PayInfo queryByOrderId(@RequestParam String orderId) {
        log.info("查询支付记录...");
        return payServiceImpl.queryByOrderId(orderId);
    }
}
