package com.ibook.pay.enums;

import com.lly835.bestpay.enums.BestPayTypeEnum;
import lombok.Getter;

@Getter
public enum PayPlatformEnum {
    //1-支付宝,2-微信
    ALIPAY(1),
    WX(2),
    ;
    Integer code;

    PayPlatformEnum(Integer code) {
        this.code = code;
    }

    public static PayPlatformEnum getByBestPayTypeEnum(BestPayTypeEnum bestPayTypeEnum) {
        //这里的代码很臃肿,重复的代码在判断,所以更好的方式是遍历这个枚举并看哪个参数与传入的参数中对应的
        //支付方式相同
//        if (bestPayTypeEnum.getPlatform().name().equals(PayPlatformEnum.ALIPAY.name())) {
//            return PayPlatformEnum.ALIPAY;
//        }else if (bestPayTypeEnum.getPlatform().name().equals(PayPlatformEnum.WX.name())) {
//            return PayPlatformEnum.WX;
//        }
        for (PayPlatformEnum value : PayPlatformEnum.values()) {
            if (bestPayTypeEnum.getPlatform().name().equals(value.name())){
                return value;
            }
        }
        //如果遍历完了还没发现,就抛出异常
        throw new RuntimeException("错误的支付平台" + bestPayTypeEnum.name());
    }
}
