package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0/5 * * * * ?")
    //@Scheduled(cron = "0 * * * * ? ") //每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        // select * from orders where status = ? and order_time < (当前时间-15分钟)
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(orders != null && orders.size()>0){
            for(Orders o :orders){
                o.setStatus(Orders.CANCELLED);
                o.setCancelReason("订单超时，自动取消");
                o.setCancelTime(LocalDateTime.now());
                orderMapper.update(o);
            }
        }
    }

    /**
     * 处理一直处理派送中的订单
     */
    @Scheduled(cron = "2/5 * * * * ?")
    //@Scheduled(cron = "0 0 1 * * ?") // 每天凌晨一点
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单： {}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(orders != null && orders.size()>0){
            for(Orders o :orders){
                o.setStatus(Orders.COMPLETED);
                orderMapper.update(o);
            }
        }
    }

}
