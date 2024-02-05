package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.entity.User;
import com.guoqz.reggie.mapper.UserMapper;
import com.guoqz.reggie.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
