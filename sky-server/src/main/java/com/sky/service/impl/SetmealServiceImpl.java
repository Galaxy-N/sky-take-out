package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐和对应的菜品
     *
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 套餐和对应菜品的信息保存在setmeal_dish表中

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 保存套餐的基本信息
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();
        // 保存套餐对应的菜品
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        if(setmealDishes!=null && setmealDishes.size()>0){
            for(SetmealDish setmealDish: setmealDishes){
                setmealDish.setSetmealId(setmealId);
            }
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */

    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 开始分页查询
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐的批量删除
     *
     * @param ids
     * @return
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        setmealDishMapper.deleteBySetmealIds(ids);

        setmealMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询套餐和对应的菜品
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        SetmealDTO setmealDTO = setmealMapper.queryById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishBySetmealId(id);
        setmealDTO.setSetmealDishes(setmealDishes);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmealDTO, setmealVO);
        return setmealVO;
    }

    /**
     * 根据套餐id修改套餐基本信息和对应的菜品
     *
     * @param setmealDTO
     */
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Long setmealId=setmealDTO.getId();
        // 删除旧的菜品
        setmealDishMapper.deleteBySetmealId(setmealId);

        // 更新新的菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for(SetmealDish setmealDish: setmealDishes){
            setmealDish.setSetmealId(setmealId);
        }
        setmealDishMapper.insertBatch(setmealDishes);

        // 更新套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);
    }

    /**
     * 修改套餐起售和停售状态
     *
     * @param status
     * @param id
     */
    @AutoFill(value = OperationType.UPDATE)
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal=Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }
}
