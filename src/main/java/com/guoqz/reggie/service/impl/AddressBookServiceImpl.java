package com.guoqz.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guoqz.reggie.entity.AddressBook;
import com.guoqz.reggie.mapper.AddressBookMapper;
import com.guoqz.reggie.service.AddressBookService;
import org.springframework.stereotype.Service;

@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
}
