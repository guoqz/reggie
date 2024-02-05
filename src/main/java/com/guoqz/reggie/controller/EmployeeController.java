package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.entity.Employee;
import com.guoqz.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 登录
     *
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        // 加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        // 判断为空
        if (emp == null) {
            return R.error("用户不存在");
        }

        // 密码校验
        if (!emp.getPassword().equals(password)) {
            return R.error("密码错误");
        }

        // 查看账号状态
        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }

        // 登录成功，将id存入 session并返回登录成功结果
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }


    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        // 清理当前 session中已登录的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }


    /**
     * 添加员工
     *
     * @param request
     * @param employee
     * @return
     */
    @RequestMapping
    public R<String> addEmployee(HttpServletRequest request, @RequestBody Employee employee) {

        // 设置初始密码，并加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        // 已设置自动填充
/*
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // 获得当前登录的 id （操作者）
        Long empId = (Long) request.getSession().getAttribute("employee");

        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);
*/
        employeeService.save(employee);

        return R.success("新增员工成功");
    }


    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 分页对象
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        // 条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // name 不等于空，该条件才添加
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        // 查询
        Page<Employee> employeePage = employeeService.page(pageInfo, queryWrapper);

        return R.success(employeePage);
    }


    /**
     * 修改状态启用|禁用状态
     *
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {

        // 已设置自动填充
/*
        // 设置更新时间和操作者（id）
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(empId);
*/
        employeeService.updateById(employee);
        return R.success("员工信息更新成功");
    }


    /**
     * 根据id查询员工信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            R.error("没查到");
        }
        return R.success(employee);
    }
}
