package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoqz.reggie.common.BaseContext;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.dto.OrdersDto;
import com.guoqz.reggie.entity.OrderDetail;
import com.guoqz.reggie.entity.Orders;
import com.guoqz.reggie.entity.ShoppingCart;
import com.guoqz.reggie.service.OrderDetailService;
import com.guoqz.reggie.service.OrdersService;
import com.guoqz.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("order")
@Slf4j
public class OrderController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> pageDtoInfo = new Page<>();// 用于返回值

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        // 这里是直接把当前用户分页的全部结果查询出来，要添加用户id作为查询条件，否则会出现用户可以查询到其他用户的订单情况
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);

        ordersService.page(pageInfo, queryWrapper);

        // 通过OrderId查询对应的OrderDetail
        LambdaQueryWrapper<OrderDetail> queryWrapper2 = new LambdaQueryWrapper<>();

        // 对OrderDto进行需要的属性赋值
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> ordersDtoList = records.stream().map(item -> {
            OrdersDto ordersDto = new OrdersDto();
            //此时的orderDto对象里面orderDetails属性还是空 下面准备为它赋值
            Long orderId = item.getId();
            List<OrderDetail> orderDetails = orderDetailService.getOrderDetailListByOrderId(orderId);
            BeanUtils.copyProperties(item, ordersDto);
            // 对orderDto进行OrderDetails属性的赋值
            ordersDto.setOrderDetails(orderDetails);
            return ordersDto;
        }).collect(Collectors.toList());

        BeanUtils.copyProperties(pageInfo, pageDtoInfo, "records");
        pageDtoInfo.setRecords(ordersDtoList);

        return R.success(pageDtoInfo);
    }


    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Orders::getOrderTime);

        ordersService.page(pageInfo, queryWrapper);

        // 未完

        return R.success(pageInfo);
    }


    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        if (null == orders) {
            return R.error("请求异常");
        }

        ordersService.submit(orders);

        return R.success("下单成功");
    }


    /**
     * 再来一单
     * 前端点击再来一单是直接跳转到购物车的，所以为了避免数据有问题，再跳转之前我们需要把购物车的数据给清除
     * ①通过orderId获取订单明细
     * ②把订单明细的数据的数据塞到购物车表中，不过在此之前要先把购物车表中的数据给清除(清除的是当前登录用户的购物车表中的数据)，
     * 不然就会导致再来一单的数据有问题；
     * (这样可能会影响用户体验，但是对于外卖来说，用户体验的影响不是很大，电商项目就不能这么干了)
     *
     * @return
     */
    @PostMapping("/again")
    public R<String> againSubmit(@RequestBody Map<String, String> map) {

        log.info(map.toString());

        // 通过用户id把原来的购物车给清空
        shoppingCartService.clean();

        // 获取传入的参数 id
        long orderId = Long.parseLong(map.get("id"));

        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        //获取该订单对应的所有的订单明细表
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);

        // 处理购物车数据
        List<ShoppingCart> shoppingCartList = orderDetailList.stream()
                .map(item -> {
                    //把从order表中和order_details表中获取到的数据赋值给这个购物车对象
                    ShoppingCart shoppingCart = new ShoppingCart();
                    shoppingCart.setUserId(BaseContext.getCurrentId());
                    shoppingCart.setImage(item.getImage());
                    Long dishId = item.getDishId();
                    if (dishId != null) {
                        //如果是菜品那就添加菜品的查询条件
                        shoppingCart.setDishId(dishId);
                    }
                    Long setmealId = item.getSetmealId();
                    if (setmealId != null) {
                        //添加到购物车的是套餐
                        shoppingCart.setSetmealId(setmealId);
                    }
                    shoppingCart.setName(item.getName());
                    shoppingCart.setDishFlavor(item.getDishFlavor());
                    shoppingCart.setNumber(item.getNumber());
                    shoppingCart.setAmount(item.getAmount());
                    shoppingCart.setCreateTime(LocalDateTime.now());
                    return shoppingCart;
                }).collect(Collectors.toList());

        shoppingCartService.saveBatch(shoppingCartList);

        return R.success("操作成功");
    }


    /**
     * 后台订单状态修改
     * <p>
     * { "status": 3, "id": "1617794332644429825"}
     *
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> orderStatusChange(@RequestBody Orders orders) {
        log.info(orders.toString());    // { "status": 3, "id": "1617794332644429825"}

        if (orders.getId() == null || orders.getStatus() == null) {
            return R.error("操作异常");
        }

        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId, orders.getId());
        updateWrapper.set(Orders::getStatus, orders.getStatus());

        ordersService.update(updateWrapper);

        return R.success("订单状态修改成功");
    }

}
