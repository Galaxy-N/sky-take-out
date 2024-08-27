package com.sky.controller.user;

import com.sky.config.RedisConfiguration;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 构造Redis中的key：dish_{category_id}
        String key="dish_"+categoryId;

        // 查询Redis中是都存在菜品数据
        List<DishVO> dishes = (List<DishVO>) redisTemplate.opsForValue().get(key); //放到Redis中的是什么类型，取出的就是什么类型
        if (dishes != null && dishes.size()>0){
            // 如果存在，直接返回，无需查询数据库
            log.info("从Redis中查询：{}", categoryId);
            return Result.success(dishes);
        }

        // 如果不存在，查询数据库，讲过查询的数据放入Redis
        log.info("从数据库中查询：{}", categoryId);
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        dishes = dishService.listWithFlavor(dish);

        redisTemplate.opsForValue().set(key, dishes);
        return Result.success(dishes);
    }

}
