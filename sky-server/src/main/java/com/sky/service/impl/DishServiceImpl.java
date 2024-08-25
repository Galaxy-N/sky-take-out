package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional
    // 涉及到菜品表和口味表的操作，多表操作需要保证事务的一致性。保证整个方法是完整性的
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 向菜品表插入一条数据
        dishMapper.insert(dish); // 在mapper中，将产生的主键id赋值给实体对象了

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        // 向口味表插入N条数据
        List<DishFlavor> flavorList = dishDTO.getFlavors();
        if (flavorList != null && flavorList.size()>0){
            flavorList.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavorList);
        }
    }


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        // 开始分页
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品的批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids){
        // 判断当前菜品是否能够删除。是否起售
        for(Long id: ids){
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                // 当前菜品on sale，不能被删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            // 当前菜品被套餐关联，不能被删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        /*
        for(Long id : ids){
            // 删除菜品表中的菜品数据
            dishMapper.deleteById(id);

            // 删除口味表中的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
        */
        // 根据菜品id批量删除菜品
        dishMapper.deleteByIds(ids);
        // 根据菜品id批量删除关联的口味
        dishFlavorMapper.deleteByDishIds(ids);
    }


    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id){
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 根据菜品id查询口味数据
        List<DishFlavor> dishFlavorList = dishFlavorMapper.getByDishId(id);
        // 将查询到的数据封装到VO
        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavorList);
        return dishVO;
    }

    /**
     * 根据菜品id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改菜品表基本信息
        dishMapper.update(dish);

        // 删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 重新插入口味数据
        List<DishFlavor> flavorList = dishDTO.getFlavors();
        if(flavorList != null && flavorList.size()>0){
            flavorList.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavorList);
        }


    }

    /**
     * 修改菜品起售和停售状态
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        // 如果是停售操作，需要将包含当前菜品的套餐也停售
        if(status == StatusConstant.DISABLE){
            List<Long> setmealDishes = setmealDishMapper.getSetmealIdsByDishId(id);
            log.info("包含当前菜品的套餐有： {}", setmealDishes);
            for(Long setmealId : setmealDishes){
                Setmeal setmeal = Setmeal.builder()
                        .id(setmealId)
                        .status(status)
                        .build();
                setmealMapper.update(setmeal);
            }
        }
    }

    public List<DishDTO> queryByCategoryId(Integer categoryId){
        List<Dish> dishes = dishMapper.queryByCategoryId(categoryId);
        List<DishDTO> dishDTOS = new ArrayList<>();
        for(Dish dish:dishes){
            DishDTO dishDTO = new DishDTO();
            BeanUtils.copyProperties(dish, dishDTO);
            dishDTOS.add(dishDTO);
        }
        return dishDTOS;
    }
}
