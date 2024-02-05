package com.guoqz.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoqz.reggie.common.R;
import com.guoqz.reggie.entity.User;
import com.guoqz.reggie.service.UserService;
import com.guoqz.reggie.utils.SMSUtils;
import com.guoqz.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        // 获取手机号
        String phone = user.getPhone();

        if (StringUtils.isNotEmpty(phone)) {
            // 生成验证码
            String code = ValidateCodeUtils.generateValidateCode4String(4);
            log.info("验证码code = {}", code);

            // 调用阿里云的短信服务api完成发送短信
//            SMSUtils.sendMessage("瑞吉外卖", "", phone, code);

            // 将验证码存入session中
            session.setAttribute(phone, code);

            return R.success("手机验证码发送成功");
        }

        return R.error("手机验证码发送失败");
    }


    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());

        // 获取输入的手机号
        String phone = map.get("phone").toString();

        // 获取输入的验证码
        String code = map.get("code").toString();

        // 从session中获取保存的验证码
        Object codeInSession = session.getAttribute("phone");

        // 比对验证码
        if (null != codeInSession && !codeInSession.equals(code)) {
            return R.error("验证码错误");
        }

        // 判断手机号对应的用户是否存在，不存在则完成自动注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);

        User user = userService.getOne(queryWrapper);
        if (null == user) {
            // 注册新用户
            user = new User();
            user.setPhone(phone);
            user.setStatus(1);
            userService.save(user);
        }
        session.setAttribute("user", user.getId());
        return R.success(user);
    }


    @PostMapping("/loginout")
    public R<String> loginOut(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }

}
