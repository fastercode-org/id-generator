package org.fastercode.idgenerator.core.generator;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ID implements Serializable {
    private static final long serialVersionUID = 6555484395667052945L;

    /**
     * 64bit位的订单ID
     */
    private long long64;

    /**
     * 字符串类型的订单号, 不包含 ExtraData
     */
    private String str;

    /**
     * 字符串类型的订单号, 包含 ExtraData
     */
    private String strWithExtraData;

    /**
     * ID生成的时间(精确到秒)
     */
    private Date createDate;
}
