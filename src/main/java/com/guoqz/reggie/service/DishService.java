package com.guoqz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoqz.reggie.dto.DishDto;
import com.guoqz.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    public void saveWithFlavor(DishDto dishDto);

    public DishDto getByIdWithFlavor(Long id);

    public void updateWithFlavor(DishDto dishDto);

    void deleteByIds(List<Long> ids);
}
