package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建AliOSSUtil
 */
@Configuration
@Slf4j
public class OSSConfiguration {
    @Bean
    // 加bean注解，项目启动的时候，就会调用这个方法，把对象创建出来。创建出来之后，交给Spring容器管理
    @ConditionalOnMissingBean
    // 这个注解的意思是，对象只创建一次，如果别的地方已经创建了，就不需要再创建了
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){  //通过参数注入的方式，直接将对象注入进来
        log.info("开始创建阿里云文件上传工具类对象：{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
