package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 统计指定时间区间内的营业额数值
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>(); //存放begin到end这个范围内的每天的日期
        dateList.add(begin);
        while (!begin.equals(end)){
            // 日期计算，计算指定日期的后一天
            begin = begin.plusDays(1);
            log.info("begin: {}", begin);
            dateList.add(begin);
        }
        String dates = StringUtils.join(dateList, ",");
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date: dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            // select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            turnoverList.add(turnover);
        }

        String t = StringUtils.join(turnoverList, ",");
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dates)
                .turnoverList(t)
                .build();
        return  turnoverReportVO;

    }
}
