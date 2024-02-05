package com.guoqz.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoqz.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    void clean();
}
