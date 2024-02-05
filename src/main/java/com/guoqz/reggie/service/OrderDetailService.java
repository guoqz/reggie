package com.guoqz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoqz.reggie.entity.OrderDetail;

import java.util.List;

public interface OrderDetailService extends IService<OrderDetail> {


    List<OrderDetail> getOrderDetailListByOrderId(Long orderId);
}
