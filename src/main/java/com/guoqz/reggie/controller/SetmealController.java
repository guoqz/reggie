package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.dto.DishDto;
import com.guoqz.reggie.dto.SetmealDto;
import com.guoqz.reggie.entity.Category;
import com.guoqz.reggie.entity.Dish;
import com.guoqz.reggie.entity.Setmeal;
import com.guoqz.reggie.entity.SetmealDish;
import com.guoqz.reggie.service.CategoryService;
import com.guoqz.reggie.service.DishService;
import com.guoqz.reggie.service.SetmealDishService;
import com.guoqz.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishService dishService;


    @PostMapping
    public R<String> add(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }


    /**
     * 套餐分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        Page<Setmeal> pageInfo = new Page<>(page, pageSize);    // 分页构造器
        Page<SetmealDto> setmealDtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);    // 模糊查询
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);   // 根据更新时间降序

        setmealService.page(pageInfo, queryWrapper);

        // 对象拷贝，忽略records 属性，单独处理
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");

        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map(item -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);  // 对象拷贝

            // 根据分类id获取分类对象
            Category category = categoryService.getById(item.getCategoryId());
            if (category != null) {
                // 设置属性值
                setmealDto.setCategoryName(category.getName());
            }
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);
    }


    /**
     * 删除套餐
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids：{}", ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }


    /**
     * 对菜品批量或者是单个 进行停售或者是起售
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        setmealService.updateSetmealStatusById(status, ids);
        return R.success("售卖状态修改成功");
    }


    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }


    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        if (null == setmealDto) {
            return R.error("请求异常");
        }

        if (null == setmealDto.getSetmealDishes()) {
            return R.error("未添加菜品信息，重新修改套餐");
        }

        setmealService.updateWithDish(setmealDto);

        return R.success("套餐修改成功");
    }


    /**
     * 根据条件查询套餐数据
     *
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != setmeal.getCategoryId(), Setmeal::getCategoryId, setmeal.getCategoryId())
//                .eq(null != setmeal.getStatus(), Setmeal::getStatus, 1);
                .eq(null != setmeal.getStatus(), Setmeal::getStatus, setmeal.getStatus())
                .orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }


    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 这里返回的是dto 对象，因为前端需要copies这个属性
     * 前端主要要展示的信息是:套餐中菜品的基本信息，图片，菜品描述，以及菜品的份数
     *
     * @param setmealId
     * @return
     */
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long setmealId) {

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);
        //获取套餐里面的所有菜品  这个就是SetmealDish表里面的数据
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        List<DishDto> dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            // 把套餐中的菜品的基本信息填充到dto中，比如菜品描述，菜品图片等菜品的基本信息
            BeanUtils.copyProperties(item, dishDto);

            Dish dish = dishService.getById(item.getDishId());

            BeanUtils.copyProperties(dish, dishDto);
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }
}
