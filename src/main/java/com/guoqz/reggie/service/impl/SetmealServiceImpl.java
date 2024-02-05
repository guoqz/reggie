package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.common.CustomException;
import com.guoqz.reggie.dto.SetmealDto;
import com.guoqz.reggie.entity.Setmeal;
import com.guoqz.reggie.entity.SetmealDish;
import com.guoqz.reggie.mapper.SetmealMapper;
import com.guoqz.reggie.service.SetmealDishService;
import com.guoqz.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        // 保存基本信息，操作setmeal表，insert操作
        setmealService.save(setmealDto);

        // 保存套餐和菜品的关联信息，操作setmeal_dish表，执行insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map(item -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    public void updateSetmealStatusById(Integer status, List<Long> ids) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ids != null, Setmeal::getId, ids);
        List<Setmeal> list = setmealService.list(queryWrapper);
        for (Setmeal setmeal : list) {
            if (setmeal != null) {
                setmeal.setStatus(status);
                setmealService.updateById(setmeal);
            }
        }
    }

    @Override
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = setmealService.getById(id);

        SetmealDto setmealDto = new SetmealDto();

        BeanUtils.copyProperties(setmeal, setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishes);

        return setmealDto;
    }

    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {

        setmealService.updateById(setmealDto);

        // 清理当前套餐关联的菜品信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getDishId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        // 添加提交的菜品信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream()
                .map(item -> {
                    item.setSetmealId(setmealDto.getId());
                    return item;
                }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);

    }

    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        // 查询套餐状态，判断是否可以删除
        // select count(*) from setmeal where id in(1,2,3) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);

        int count = setmealService.count(queryWrapper);
        if (count > 0) {
            // 如果不能删除，抛出业务异常
            throw new CustomException("套餐售卖中，不可删除");
        }

        // 可以删除，先删除套餐表中的数据      setmeal
        setmealService.removeByIds(ids);

        // 在删除关系表中的数据       setmeal_dish
        // delete from setmeal_dish where setmeal_id in (1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }
}
