package com.codereview.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 用于API接口的标准化响应格式
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 私有构造函数
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result实例
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result实例
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(null);
        return result;
    }

    /**
     * 创建成功响应（仅消息，无数据）
     * 用于返回 Result<Void>
     *
     * @param message 成功消息
     * @return Result<Void>实例
     */
    public static Result<Void> success(String message) {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    /**
     * 创建成功响应（带数据和消息）
     * 兼容调用方传入 (data, message) 的顺序
     *
     * @param data    响应数据
     * @param message 成功消息
     * @param <T>     数据类型
     * @return Result<T>实例
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 创建错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 创建错误响应（使用默认错误码5000）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> error(String message) {
        return error(5000, message);
    }

    /**
     * 判断是否成功
     *
     * @return true表示成功
     */
    public boolean isSuccess() {
        return this.code == 200;
    }
}