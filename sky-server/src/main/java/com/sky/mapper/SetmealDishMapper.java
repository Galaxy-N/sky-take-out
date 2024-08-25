package com.sky.mapper;

import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据多个菜品id，查询对应的多个套餐id
     * @param dishIds
     * @return
     */
    // select setmeal_id from setmeal_dish where dish_id in dishIds
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 根据菜品id查询对应的多个套餐
     * @param dishId
     * @return
     */
    List<Long> getSetmealIdsByDishId(Long dishId);

    /**
     * 批量插入菜品和套餐对应数据
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据菜品id删除套餐
     * @param dishId
     */
    @Delete("delete from setmeal_dish where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 根据菜品id批量删除套餐
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根据id删除套餐
     * @param id
     */
    @Delete("delete from setmeal_dish where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据套餐id批量删除套餐
     * @param ids
     */
    void deleteBySetmealIds(List<Long> ids);

    /**
     * 根据套餐id查询套餐-菜品关系
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getSetmealDishBySetmealId(Long setmealId);

    /**
     * 根据菜品id删除套餐
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

}
