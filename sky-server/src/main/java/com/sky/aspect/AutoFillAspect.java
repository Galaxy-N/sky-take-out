package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    // 切面=切入点+通知

    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill) ")
    // 定义哪些方法会被拦截。这里需要同时满足两个条件。首先是操作数据库的函数，即mapper中的函数，其次需要被AutoFill注解
    // 拦截之后要为公共的字段赋值
    public void autoFillPointCut(){

    }

    /**
     * 前置通知，在通知中进行公共字段的赋值
     */
    // 通知有多种：前置通知，后置通知，环绕通知，异常通知
    // 这里应该是前置通知，因为要在执行insert和update之前，就为公共字段赋值
    @Before("autoFillPointCut()")
    // 在autoFillPointCut匹配的切点之前触发
    // before表明要在注解标记的函数之前触发通知
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段字段填充...");

        /*
        * autoFill执行的时候，拦截是mapper中的方法，mapper中的函数加上了AutoFill注解。
        * mapper函数加注解的时候，还要指定当前数据库操作类型是insert还是update，不同的操作类型，公共字段的赋值是不一样的
        * 1. 获取当前被拦截的方法上的数据操作类型
        * 2. 获取当前被拦截的方法参数（实体对象：员工、菜品、套餐、分类）
        * 3. 准备赋值的数据（时间、用户id）
        * 4. 根据当前不同的操作类型，为对应的属性通过反射来赋值
        *
        * */
        // 1. 获取当前被拦截的方法上的数据操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();  //方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); //获得方法上的注解对象
        OperationType operationType = autoFill.value();     //获得数据库操作类型

        // 2. 获取当前被拦截的方法参数（实体对象：员工、菜品、套餐、分类）
        Object[] args = joinPoint.getArgs();
        if( args == null || args.length == 0){
            return; //防止出现空指针
        }
        Object entity = args[0];  // 实体对象出现在参数的第0个位置
        // 3. 准备赋值的数据（时间、用户id）
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 4. 根据当前不同的操作类型，为对应的属性通过反射来赋值
        // 调用set方法赋值
        if(operationType == OperationType.INSERT){
            // 为4个公共字段赋值
            try {
                // 获取set方法
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射未对象属性赋值
                setCreateTime.invoke(entity, now);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
                setCreateUser.invoke(entity, currentId);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(operationType == OperationType.UPDATE){
            // 为2个公共字段赋值
            try {
                // 获取set方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射未对象属性赋值
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
