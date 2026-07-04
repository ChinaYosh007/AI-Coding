package com.yosh.coding.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yosh.model.dto.user.UserDTO;
import com.yosh.model.dto.user.UserQueryRequest;
import com.yosh.model.dto.user.UserRegisterRequest;
import com.yosh.model.dto.user.UserUpdateMyRequest;
import com.yosh.model.dto.user.UserUpdatePasswordRequest;
import com.yosh.model.entity.User;
import com.yosh.model.vo.LoginUserVO;
import com.yosh.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-17
 */
public interface UserService extends IService<User> {

    Long registerUser(UserRegisterRequest userRegisterRequest);

    /**
     * 获取脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(UserDTO user, HttpServletRequest httpServletRequest);

    /**
     * 获取当前登录用户
     */
    LoginUserVO getLoginUser(HttpServletRequest request);

    void userLogout(HttpServletRequest request);

    boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request);

    boolean updateMyPassword(UserUpdatePasswordRequest userUpdatePasswordRequest, HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);
}
