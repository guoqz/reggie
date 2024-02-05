package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoqz.reggie.common.BaseContext;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.entity.ShoppingCart;
import com.guoqz.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;


    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        log.info("查看购物车。。。");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }


    /**
     * 添加购物车商品
     *
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("shoppingCart:{}", shoppingCart);

        // 设置数据
        // 设置用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);

        // 查询当前的菜品或者套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        if (dishId != null) {
            // 添加的是菜品
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        }
        if (setmealId != null) {
            // 添加的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }

        // 查询菜品或套餐是否在购物车中
        // select * from shopping_cart where user_id = ? and dish_id = ?
        // select * from shopping_cart where user_id = ? and  setmeal_id = ?
        ShoppingCart shoppingCartOne = shoppingCartService.getOne(queryWrapper);
        if (null != shoppingCartOne) {
            // 购物车中存在，购物车中商品数量+1
            shoppingCartOne.setNumber(shoppingCartOne.getNumber() + 1);
            shoppingCartService.updateById(shoppingCartOne);
        } else {
            // 不存在，默认设置为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            shoppingCartOne = shoppingCart;
        }

        return R.success(shoppingCartOne);
    }


    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        // 当前用户
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        // 判断是菜品减少还是套餐减少
        if (null != shoppingCart.getDishId()) {
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }

        if (null != shoppingCart.getSetmealId()) {
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        ShoppingCart shoppingCart1 = shoppingCartService.getOne(queryWrapper);

        shoppingCart1.setNumber(shoppingCart1.getNumber() - 1);// 减库存
        Integer currentNum = shoppingCart1.getNumber();// 获取当前库存
        if (0 < currentNum) {
            // 更新操作
            shoppingCartService.updateById(shoppingCart1);

        } else if (0 == currentNum) {
            // 购物车商品减为0，直接删除该商品
            shoppingCartService.removeById(shoppingCart1);

//        } else if (0 > currentNum) {
        } else {
            return R.error("操作异常");
        }

        return R.success(shoppingCart1);

    }


    @DeleteMapping("/clean")
    public R<String> clean() {

        shoppingCartService.clean();

        return R.success("购物车已清空");
    }


}


