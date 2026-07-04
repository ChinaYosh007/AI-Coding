package com.yosh.coding.aop;

import cn.hutool.core.util.StrUtil;
import com.yosh.coding.annotation.AuthCheck;
import com.yosh.coding.service.UserService;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.enums.UserRoleEnum;
import com.yosh.model.vo.LoginUserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthAOP {
    @Resource
    private UserService userService;

    @Pointcut("@annotation(com.yosh.coding.annotation.AuthCheck)")
    public void point(){}

    @Around("point()")
    public Object doBefore(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //获取被拦截方法的签名信息 ----> Signature
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        // 通过Method反射拿到@AuthCheck对象的实例
        AuthCheck authCheck = signature.getMethod().getAnnotation(AuthCheck.class);
        // 获取用户权限
        String role = authCheck.mustRole();
        if(StrUtil.isBlank(role)) return proceedingJoinPoint.proceed();
        // 校验权限
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        LoginUserVO loginUser = userService.getLoginUser(request);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if(userRoleEnum == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);

        if(!role.equals(userRoleEnum.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return proceedingJoinPoint.proceed();

    }
}
