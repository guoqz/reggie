package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.common.CustomException;
import com.guoqz.reggie.dto.DishDto;
import com.guoqz.reggie.entity.Dish;
import com.guoqz.reggie.entity.DishFlavor;
import com.guoqz.reggie.mapper.DishMapper;
import com.guoqz.reggie.service.DishFlavorService;
import com.guoqz.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对应的其他数据
     *
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品基本信息
//        this.save(dishDto);
        dishService.save(dishDto); // 同上

        Long dishId = dishDto.getId(); // 菜品id
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().
                map(item -> {
                    item.setDishId(dishId);
                    return item;
                }).collect(Collectors.toList());

        // 保存菜品口味到对应表中 dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息及关联口味信息
     *
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        // 查询菜品基本信息，从dish表查
        Dish dish = dishService.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        // 获取该菜品关联的口味信息，从dish_flavor表查
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息
     *
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        // 更新dish表基本信息
        dishService.updateById(dishDto);

        // 清理当前菜品关联的口味信息    --- dish_flavor表  delete 操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);

        // 添加提交的口味数据    --- dish_flavor表  insert 操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream()
                .map(item -> {
                    item.setDishId(dishDto.getId());
                    return item;
                }).filter(flavor -> !"".equals(flavor.getName()))
                .collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 先查询该菜品是否在售卖，如果是则抛出业务异常
        queryWrapper.in(ids != null, Dish::getId, ids);
        List<Dish> list = dishService.list(queryWrapper);
        for (Dish dish : list) {
            if (0 == dish.getStatus()) {
                dishService.removeById(dish.getId());
            } else {
                throw new CustomException("菜品正在售卖，删除失败");
            }
        }

    }

}
