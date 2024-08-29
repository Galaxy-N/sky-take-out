package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.OrderDetail;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import com.sky.vo.TurnoverReportVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "商家订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/details/{id}")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("根据id查看订单详情：{}", id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){
        log.info("统计各个状态的订单数量");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("取消订单: {}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);

        return Result.success();
    }

    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单：{}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        log.info("拒绝订单: {}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);

        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable Long id){
        log.info("派送订单：{}", id);
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable Long id){
        log.info("完成订单：{}", id);
        orderService.complete(id);
        return Result.success();
    }

}
