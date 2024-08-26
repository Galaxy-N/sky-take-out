package com.sky.service;

import com.sky.result.Result;

public interface ShopService {
    /**
     * 设置店铺的营业状态
     * @param status
     */
    void setStatus(Integer status);

    /**
     * 获取店铺的营业状态
     * @return
     */
    Integer getStatus();
}
