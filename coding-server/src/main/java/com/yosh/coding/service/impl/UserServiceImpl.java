package com.yosh.coding.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yosh.exception.BusinessException;
import com.yosh.exception.ErrorCode;
import com.yosh.model.costants.UserContants;
import com.yosh.model.dto.user.UserDTO;
import com.yosh.model.dto.user.UserQueryRequest;
import com.yosh.model.dto.user.UserRegisterRequest;
import com.yosh.model.entity.User;
import com.yosh.coding.mapper.UserMapper;
import com.yosh.coding.service.UserService;
import com.yosh.model.enums.UserRoleEnum;
import com.yosh.model.vo.LoginUserVO;
import com.yosh.model.vo.UserVO;
import com.yosh.utils.PrefixUtil;
import com.yosh.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 服务层实现。
 *
 * @author yaoxi_rf7anxm
 * @since 2026-06-17
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Long registerUser(UserRegisterRequest userRegisterRequest) {
        if (StrUtil.hasBlank(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword()))
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        if (!userRegisterRequest.getUserPassword().equals(userRegisterRequest.getCheckPassword()))
            throw new BusinessException(ErrorCode.PASSWORD_DIFFERENCE);

        User user = BeanUtil.copyProperties(userRegisterRequest, User.class);
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, user.getUserAccount());
        Long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) throw new BusinessException(ErrorCode.USER_ALREARLY_HAVE);

        user.setUserPassword(UserUtils.getEncryptPassword(user.getUserPassword()));
        String nickname = UUID.randomUUID().toString(true);
        user.setUserName(PrefixUtil.USER_PREFIX + nickname);
        user.setUserRole(UserRoleEnum.USER.getValue());

        Boolean res = this.save(user);
        if (BooleanUtil.isFalse(res)) throw new BusinessException(ErrorCode.ADD_USER_ERROR);
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(UserDTO userDTO, HttpServletRequest httpServletRequest) {
        if (userDTO == null) throw new BusinessException(ErrorCode.OPERATION_ERROR);

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userDTO.getUserAccount())
                .eq(User::getUserPassword, UserUtils.getEncryptPassword(userDTO.getUserPassword()));
        User user = mapper.selectOneByQuery(queryWrapper);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);

        httpServletRequest.getSession().setAttribute(UserContants.USER_LOGIN_STATE, user);

        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserContants.USER_LOGIN_STATE);
        if (user == null || user.getId() == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    @Override
    public void userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(UserContants.USER_LOGIN_STATE) == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        request.removeAttribute(UserContants.USER_LOGIN_STATE);
    }
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest){
        if(userQueryRequest == null) throw new BusinessException(ErrorCode.ERROR_QUERY,"查询失败");
         return  this.query().eq(User::getId,userQueryRequest.getId())
                .eq(User::getUserRole,userQueryRequest.getUserRole())
                .like(User::getUserAccount,userQueryRequest.getUserAccount())
                .like(User::getUserName,userQueryRequest.getUserName())
                .like(User::getUserProfile,userQueryRequest.getUserProfile())
                .orderBy(userQueryRequest.getSortField(),"ascend".equals(userQueryRequest.getSortOrder()));
    }

}
