package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){
        // 1. 处理各种业务异常
        // 地址簿为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            // 抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        // 购物车为空
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if(shoppingCarts == null || shoppingCarts.size() <=0){
            // 抛出购物车为空的异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2. 向订单表中插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setAddress(addressBook.getDetail());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        // 3. 向订单明细表中插入多条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart cart: shoppingCarts){
            OrderDetail orderDetail = new OrderDetail(); //订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());   // 设置当前订单明细关联的订单id
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        // 4. 清空当前购物车数据
        shoppingCartMapper.deleteAllByUserId(userId);

        // 封装VO返回
        OrderSubmitVO orderSubmitVO= OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        /*
        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo; */

        paySuccess(ordersPaymentDTO.getOrderNumber());



        String orderNumber = ordersPaymentDTO.getOrderNumber(); //订单号

        Long orderid = orderMapper.getorderId(orderNumber);//根据订单号查主键



        JSONObject jsonObject = new JSONObject();//本来没有2

        jsonObject.put("code", "ORDERPAID"); //本来没有3

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);

        vo.setPackageStr(jsonObject.getString("package"));

        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改

        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付

        Integer OrderStatus = Orders.TO_BE_CONFIRMED; //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新

        LocalDateTime check_out_time = LocalDateTime.now();



        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderid);

        return vo;  //  修改支付方法中的代码
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        log.info("订单支付成功==================================================");
        //通过websocket向客户端浏览器推送消息
        Map map = new HashMap<>();
        map.put("type", 1) ; // 1表示来电提醒，2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号："+outTradeNo); // 具体的提示内容
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id){
        Orders ordersDB = orderMapper.getById(id);
        Map map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号："+ordersDB.getNumber());

        // 通过websocket向浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查看订单详情
     * @param id
     * @return
     */
    public OrderVO getOrderDetail(Long id) {
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetail);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);

        // 检查订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(orders.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果在待接单状态下取消，需要给用户退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        log.info("{}",orderDetails);
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        if(orderDetails == null || orderDetails.size() ==0){
            throw new OrderBusinessException("订单信息错误！");
        }
        for(OrderDetail orderDetail:orderDetails){
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCarts.add(shoppingCart);
        }
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */

    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO){
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOS = new ArrayList<>();
        for(Orders o: page){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(o, orderVO);
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(o.getId());

            orderVO.setOrderDetailList(orderDetails);
            orderVOS.add(orderVO);
        }
        return new PageResult(page.getTotal(), orderVOS);
    }


    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        log.info("搜索订单：");
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOS= new ArrayList<>();
        for(Orders o : orders){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(o, orderVO);
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderVO.getId());
            String dishes="";
            for (OrderDetail orderDetail: orderDetails ){
                dishes+=orderDetail.getName()+"*"+orderDetail.getNumber()+";  ";
            }
            orderVO.setOrderDishes(dishes);
            orderVOS.add(orderVO);
        }
        log.info("订单查询：{}",orderVOS);
        return new PageResult(orderVOS.size(), orderVOS);
    }


    public OrderVO details(Long id){
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }


    public OrderStatisticsVO statistics(){
        Long userId = BaseContext.getCurrentId();
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(orderMapper.statistics(userId, Orders.CONFIRMED)); //待派送
        orderStatisticsVO.setDeliveryInProgress(orderMapper.statistics(userId, Orders.DELIVERY_IN_PROGRESS)); //派送中
        orderStatisticsVO.setToBeConfirmed(orderMapper.statistics(userId, Orders.TO_BE_CONFIRMED)); //待接单
        return orderStatisticsVO;
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO){
        Orders order = orderMapper.getById(ordersCancelDTO.getId());
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getPayStatus() == Orders.PAID){
            order.setPayStatus(Orders.UN_PAID);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(order);
    }

    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders order = orderMapper.getById(ordersConfirmDTO.getId());
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        order.setStatus(Orders.CONFIRMED);
        orderMapper.update(order);
    }

    public void rejection(OrdersRejectionDTO ordersRejectionDTO){
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if(order.getPayStatus() == Orders.PAID){
            // 如果用户已经支付了，为用户退款
            order.setPayStatus(Orders.UN_PAID);
        }
        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderMapper.update(order);
    }

    public void delivery(Long id){
        Orders order = orderMapper.getById(id);
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    public void complete(Long id){
        Orders order = orderMapper.getById(id);
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(order.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order.setStatus(Orders.COMPLETED);
        orderMapper.update(order);
    }
}
