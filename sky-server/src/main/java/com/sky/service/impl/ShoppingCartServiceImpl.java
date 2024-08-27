package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private  ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断加入购物车的商品是否已经在购物车中了
        ShoppingCart shoppingCart=new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);

        // 如果存在，将数量+1
        if(shoppingCarts != null && shoppingCarts.size() > 0){
            log.info("当前商品已经存在于购物车: {}", shoppingCart);
            ShoppingCart cart = shoppingCarts.get(0);
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.updateNumberById(cart);
            return;
        }
        // 如果不存在，插入

        // 判断本次添加的是菜品还是套餐
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();
        if(dishId != null){
            // 本次添加到购物车的是菜品
            Dish dish = dishMapper.getById(dishId);
            log.info("添加的是商品：{}", dish);
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        }else{
            // 本次添加到购物车的是套餐
            Setmeal setmeal = setmealMapper.getById(setmealId);
            log.info("添加的是套餐：{}", setmeal);
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        }
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(shoppingCart);
    }
}
