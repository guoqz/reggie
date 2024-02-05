package com.guoqz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoqz.reggie.dto.SetmealDto;
import com.guoqz.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时保存套餐和菜品的关联信息
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除删除套餐和菜品的关联信息
     * @param ids
     */
    public void removeWithDish(List<Long> ids);

    void updateSetmealStatusById(Integer status, List<Long> ids);

    SetmealDto getByIdWithDish(Long id);

    void updateWithDish(SetmealDto setmealDto);
}
