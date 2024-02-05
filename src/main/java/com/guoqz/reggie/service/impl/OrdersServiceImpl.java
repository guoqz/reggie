package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.common.BaseContext;
import com.guoqz.reggie.entity.*;
import com.guoqz.reggie.mapper.OrdersMapper;
import com.guoqz.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Override
    public void submit(Orders orders) {

        // 获取当前用户id
        Long currentUserId = BaseContext.getCurrentId();

        // 获取购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentUserId);

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        if (null == shoppingCarts || 0 == shoppingCarts.size()) {
            throw new RuntimeException("购物车为空，下单异常");
        }

        // 获取当前用户数据
        User user = userService.getById(currentUserId);

        // 获取地址信息
        AddressBook addressBook = addressBookService.getById(orders.getAddressBookId());
        if (null == addressBook) {
            throw new RuntimeException("请选择地址");
        }

        // 流程：
        // 向订单表插入一条数据
        // 向订单明细表插入多条数据
        // 清空购物车

        // 生成订单号
        long orderId = IdWorker.getId();
        // 总金额
        AtomicInteger total = new AtomicInteger(0);    // 原子操作，线程安全
        // 处理购物车数据      计算总金额，设置属性值
        List<OrderDetail> orderDetails = shoppingCarts.stream().map(item -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            // 计算
            total.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        // 设置订单属性值
        orders.setId(orderId);// 用订单编号做主键
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(total.get()));
        orders.setUserId(currentUserId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());// 收件人
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(
                (addressBook.getProvinceCode() == null ? "" : addressBook.getProvinceCode())
                        + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                        + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                        + (addressBook.getDetail() == null ? "" : addressBook.getDetail())
        );

        // 向订单表插入一条数据
        ordersService.save(orders);

        // 向订单明细表插入多条数据
        orderDetailService.saveBatch(orderDetails);

        // 清空购物车
        shoppingCartService.remove(queryWrapper);
    }
}
