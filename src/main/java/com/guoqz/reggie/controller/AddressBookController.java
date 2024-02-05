package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guoqz.reggie.common.BaseContext;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.entity.AddressBook;
import com.guoqz.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;


    @GetMapping("list")
    public R<List<AddressBook>> list(AddressBook addressBook) {

        addressBook.setUserId(BaseContext.getCurrentId());

        // select * from address_book where user_id = ? order by update_time desc
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);
        List<AddressBook> addressBookList = addressBookService.list(queryWrapper);

        return R.success(addressBookList);
    }


    @PostMapping
    public R<AddressBook> add(@RequestBody AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());

        log.info("addressBook:{}", addressBook);

        addressBookService.save(addressBook);
        return R.success(addressBook);
    }


    /**
     * 根据id查询地址
     */
    @GetMapping("/{id}")
    public R<AddressBook> get(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("没有找到该数据");
        }
    }


    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook) {
        if (null == addressBook) {
            return R.error("请求异常");
        }
        addressBookService.updateById(addressBook);
        return R.success("修改成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam("ids") Long id) {
        if (null == id) {
            return R.error("请求异常");
        }

//        addressBookService.removeById(id);

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getId, id)
                .eq(AddressBook::getUserId, BaseContext.getCurrentId());
        addressBookService.remove(queryWrapper);

        return R.success("地址删除成功");
    }


    /**
     * 设置默认地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public R<AddressBook> setDefaultAddress(@RequestBody AddressBook addressBook) {

        log.info("addressBook:{}", addressBook);

        if (null == addressBook) {
            return R.error("请求异常");
        }

        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();

        // update address_book set is_default = 0 where user_id = ?
        updateWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        updateWrapper.set(AddressBook::getIsDefault, 0);

        addressBookService.update(updateWrapper);

        // update address_book set is_default = 1 where id = ?
        addressBook.setIsDefault(1);

        addressBookService.updateById(addressBook);

        return R.success(addressBook);

    }


    /**
     * 查询默认地址
     */
    @GetMapping("default")
    public R<AddressBook> getDefaultAddress() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        // select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }
}
