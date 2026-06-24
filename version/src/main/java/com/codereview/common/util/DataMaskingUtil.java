package com.codereview.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 数据脱敏工具类
 */
public class DataMaskingUtil {

    /**
     * 手机号脱敏：138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    /**
     * 邮箱脱敏：zhangs***@***.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            username = username + "***";
        } else {
            username = username.substring(0, username.length() - 2) + "***";
        }

        return username + "@***." + (domain.contains(".") ? domain.substring(domain.lastIndexOf(".") + 1) : "com");
    }

    /**
     * 姓名脱敏：张三 -> 张*
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name + "*";
        }
        return name.substring(0, 1) + new String(new char[name.length() - 1]).replace('\0', '*');
    }

    /**
     * IP地址脱敏：192.168.***.***
     */
    public static String maskIp(String ip) {
        if (ip == null) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return ip;
    }

    /**
     * 身份证号脱敏：110101**********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 3) + new String(new char[length - 7]).replace('\0', '*')
                + idCard.substring(length - 4);
    }
}
