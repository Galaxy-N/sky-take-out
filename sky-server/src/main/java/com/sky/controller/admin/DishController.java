package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 清理缓存数据
        String key = "dish_"+dishDTO.getCategoryId();
        cleanCache(key);
        //redisTemplate.delete(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 菜品的批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品的批量删除品")
    public Result delete(@RequestParam List<Long> ids){  // @RequestParam注解将字符串解析成List
        log.info("菜品的批量删除: {}", ids);
        dishService.deleteBatch(ids);

        // 删除缓存需要知道category_id，如果去数据库查会得不偿失。所以将所有的菜品缓存数据都清理
        // 删除缓存不能使用通配符，所以要先查询
        cleanCache("dish_*");
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        return Result.success();
    }

    /**
     * 根据id查询菜品和对应的口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品和对应的口味")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品和对应的口味: {}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品: {}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        cleanCache("dish_*");
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        return Result.success();
    }

    /**
     * 修改菜品起售和停售状态
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "修改菜品起售和停售状态")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("修改菜品起售和停售状态");
        dishService.startOrStop(status, id);

        cleanCache("dish_*");
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<DishDTO>> queryByCategoryId(Integer categoryId){
        List<DishDTO> dishes = dishService.queryByCategoryId(categoryId);
        return Result.success(dishes);
    }

    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
