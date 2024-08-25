package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识某个方法需要进行功能字段自动填充处理
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    // 数据操作类型，update or insert
    OperationType value();      //声明一个方法，方法的返回值是枚举类型OperationType
    // 使用@AutoFill注解时，会指定一个OperationType枚举常量作为注解的值
    // @AutoFill(value = OperationType.UPDATE)，这里就是将OperationType.UPDATE作为注解的值
}
