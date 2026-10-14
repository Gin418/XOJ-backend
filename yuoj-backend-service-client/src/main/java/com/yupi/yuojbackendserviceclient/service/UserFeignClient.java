package com.yupi.yuojbackendserviceclient.service;

import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.utils.LoginUserUtil;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.UserRoleEnum;
import com.yupi.yuojbackendmodel.model.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@FeignClient(name = "yuoj-backend-user-service", path = "/api/user/inner")
public interface UserFeignClient {

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @GetMapping("/get/id")
    User getById(@RequestParam("userId") long userId);

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @GetMapping("/get/ids")
    List<User> listByIds(@RequestParam("idList") Collection<Long> idList);

    /**
     * 根据昵称模糊查询用户列表
     * @param userName
     * @return
     */
    @GetMapping("/get/userName")
    List<User> listByUserName(@RequestParam("userName") String userName);

    /**
     * 根据 token 获取登录用户
     * @param token
     * @return
     */
    @GetMapping("/get/login/token")
    User getLoginUserByToken(@RequestParam("token") String token);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    default User getLoginUser(HttpServletRequest request) {
        // 从请求头取 token，跨服务查询登录用户
        User currentUser = getLoginUserByToken(getTokenFromRequest(request));
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 可以考虑在这里做全局权限校验
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录，未登录时返回 null）
     *
     * @param request
     * @return
     */
    default User getLoginUserPermitNull(HttpServletRequest request) {
        // 从请求头取 token，跨服务查询登录用户
        User currentUser = getLoginUserByToken(getTokenFromRequest(request));
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return currentUser;
    }

    /**
     * 从请求头中获取 token（支持 Authorization: Bearer xxx）
     *
     * @param request
     * @return
     */
    static String getTokenFromRequest(HttpServletRequest request) {
        return LoginUserUtil.getTokenFromRequest(request);
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    default boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

}
