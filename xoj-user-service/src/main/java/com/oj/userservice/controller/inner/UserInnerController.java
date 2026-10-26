package com.oj.userservice.controller.inner;

import com.oj.model.entity.User;
import com.oj.serviceclient.service.UserFeignClient;
import com.oj.userservice.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class UserInnerController implements UserFeignClient {

    @Resource
    private UserService userService;

    /**
     * 根据 id 获取用户
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/get/id")
    public User getById(@RequestParam("userId") long userId) {
        return userService.getById(userId);
    }

    /**
     * 根据 id 获取用户列表
     * @param idList
     * @return
     */
    @Override
    @GetMapping("/get/ids")
    public List<User> listByIds(@RequestParam("idList") Collection<Long> idList) {
        return userService.listByIds(idList);
    }

    /**
     * 根据昵称模糊查询用户列表
     * @param userName
     * @return
     */
    @Override
    @GetMapping("/get/userName")
    public List<User> listByUserName(@RequestParam("userName") String userName) {
        return userService.lambdaQuery()
                .like(User::getUserName, userName)
                .select(User::getId, User::getUserName, User::getUserAvatar, User::getUserProfile)
                .list();
    }

    /**
     * 根据 token 获取登录用户
     * @param token
     * @return
     */
    @Override
    @GetMapping("/get/login/token")
    public User getLoginUserByToken(@RequestParam("token") String token) {
        return userService.getLoginUserByToken(token);
    }

}
