package com.guoqz.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.guoqz.reggie.common.BaseContext;
import com.guoqz.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检测用户是否已完成登录
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 获取本次请求的 uri 路径
        String requestURI = request.getRequestURI();
        log.info("拦截请求‘{}’", requestURI);

        // 定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login",
                "/user/logout"
        };

        // 判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        // 不需要处理直接放行
        if (check) {
            log.info("本次‘{}’请求不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 判断登录状态(后台端)，已登录，直接放行
        if (null != request.getSession().getAttribute("employee")) {
            log.info("id为‘{}’的用户已登录", request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");

            // 存入线程中，后续可在线程中取值
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);
            return;
        }


        // 判断登录状态（移动端），已登录，直接放行
        if (null != request.getSession().getAttribute("user")) {
            log.info("id为‘{}’的用户已登录", request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");

            // 存入线程中，后续可在线程中取值
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        log.info("用户未登录");
        // 如果未登录返回登录结果，通过输出流的方式响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    // 路径匹配器，支持通配符

    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();


    /**
     * 路径匹配，检测本次请求是否需要放行
     *
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }

}
