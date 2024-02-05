package com.guoqz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoqz.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {
    void submit(Orders orders);
}
