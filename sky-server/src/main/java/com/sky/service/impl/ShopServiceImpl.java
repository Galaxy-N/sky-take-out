package com.sky.service.impl;

import com.sky.constant.ShopStatusConstant;
import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺的营业状态
     * @param status
     */
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set(ShopStatusConstant.KEY, status);
    }

    /**
     * 获取店铺的应用状态
     * @return
     */
    public Integer getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(ShopStatusConstant.KEY);
        return status;
    }
}
