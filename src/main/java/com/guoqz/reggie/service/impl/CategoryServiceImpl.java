package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.common.CustomException;
import com.guoqz.reggie.entity.Category;
import com.guoqz.reggie.entity.Dish;
import com.guoqz.reggie.entity.Setmeal;
import com.guoqz.reggie.mapper.CategoryMapper;
import com.guoqz.reggie.service.CategoryService;
import com.guoqz.reggie.service.DishService;
import com.guoqz.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据id删除分类，删除之前判断该分类下是否关联有菜品或套餐
     *
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        int count = dishService.count(dishLambdaQueryWrapper);
        if (count > 0) {
            // 存在关联菜品
            throw new CustomException("该分类下关联了菜品，不能删除");
        }

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        int count2 = setmealService.count(setmealLambdaQueryWrapper);
        if (count2 > 0) {
            // 存在关联套餐
            throw new CustomException("该分类下关联了套餐，不能删除");
        }

//        this.removeById(id);
        categoryService.removeById(id); // 同上
    }


}
