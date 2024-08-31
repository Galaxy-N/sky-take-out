package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WorkspaceService workspaceService;
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

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        while(! begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("endTime", endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUser = totalUser == null ? 0:totalUser;
            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUser = newUser== null ? 0 : newUser;
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }
        String dates = StringUtils.join(dateList, ",");
        String newUsers = StringUtils.join(newUserList, ",");
        String totalUsers = StringUtils.join(totalUserList, ",");
        return UserReportVO.builder()
                .dateList(dates)
                .newUserList(newUsers)
                .totalUserList(totalUsers)
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        while(! begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> totalOrderList = new ArrayList<>();
        List<Integer> validOrderList = new ArrayList<>();

        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            Integer totalOrder = orderMapper.countByMap(map);
            totalOrder = totalOrder == null ? 0:totalOrder;
            map.put("status", Orders.COMPLETED);
            Integer validOrder = orderMapper.countByMap(map);
            validOrder = validOrder==null?0:validOrder;
            totalOrderList.add(totalOrder);
            validOrderList.add(validOrder);
        }
        String dates = StringUtils.join(dateList, ",");
        String validOrders = StringUtils.join(validOrderList, ",");
        String totalOrders = StringUtils.join(totalOrderList, ",");
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        for (Integer i:totalOrderList){
            totalOrderCount+=i;
        }
        for(Integer i : validOrderList){
            validOrderCount += i;
        }
        return OrderReportVO.builder()
                .dateList(dates)
                .orderCountList(totalOrders)
                .totalOrderCount(totalOrderCount)
                .validOrderCountList(validOrders)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(validOrderCount*1.0/totalOrderCount)
                .build();
    }


    /**
     * 统计指定时间区间内的top10商品
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end){

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        Map map = new HashMap<>();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("topK", 10);
        List<GoodsSalesDTO> goodsSalesDTOS = orderDetailMapper.getTop(map);
        List<String> nameList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOS.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 导出运营报表
     * @param httpServletResponse
     */
    public void exportBusinessData(HttpServletResponse httpServletResponse){
        // 1. 查询数据库，获取运营数据。查询近30天数据
        LocalDate endTime = LocalDate.now().minusDays(1);
        LocalDate beginTime = LocalDate.now().minusDays(30);
        log.info("开始时间：{}  结束时间：{}", beginTime, endTime);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(beginTime, LocalTime.MAX),
                LocalDateTime.of(endTime, LocalTime.MIN)
        );

        // 2. 通过POI将数据写入到excel文件中
        InputStream in = this.getClass().getResourceAsStream("/template/运营数据报表模板.xlsx");

        // 基于模板文件创建一个新的excel文件
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取表格
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充数据：时间
            XSSFRow row = sheet.getRow(1);
            row.getCell(1).setCellValue("时间："+beginTime+" 至 "+endTime);

            // 填入概览数据
            row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填入明细数据
            for(int i=0 ; i<30; i++){
                LocalDate date = beginTime.plusDays(i);
                // 查询某一天的营业数据
                BusinessDataVO vo = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX)
                        );
                log.info("{} {}", date, vo);
                row = sheet.getRow(7+i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(vo.getTurnover());
                row.getCell(3).setCellValue(vo.getValidOrderCount());
                row.getCell(4).setCellValue(vo.getOrderCompletionRate());
                row.getCell(5).setCellValue(vo.getUnitPrice());
                row.getCell(6).setCellValue(vo.getNewUsers());

            }

            // 3. 通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = httpServletResponse.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
