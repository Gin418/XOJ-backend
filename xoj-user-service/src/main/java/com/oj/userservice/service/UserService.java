package com.oj.userservice.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oj.model.dto.user.UserQueryRequest;
import com.oj.model.entity.User;
import com.oj.model.vo.LoginUserVO;
import com.oj.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param email         邮箱
     * @param captcha       邮箱验证码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String email, String captcha);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendCaptcha(String phone);

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱
     * @return 是否发送成功
     */
    boolean sendEmailCaptcha(String email);

    /**
     * 验证码登录（手机号未注册时自动注册）
     *
     * @param phone   手机号
     * @param code    短信验证码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userSmsLogin(String phone, String code, HttpServletRequest request);

    /**
     * 通过邮箱找回密码
     *
     * @param email         邮箱
     * @param captcha       邮箱验证码
     * @param newPassword   新密码
     * @param checkPassword 确认密码
     * @return 是否重置成功
     */
    boolean userResetPassword(String email, String captcha, String newPassword, String checkPassword);

    /**
     * 根据 token 从 Redis 中获取登录用户（未登录返回 null）
     *
     * @param token
     * @return
     */
    User getLoginUserByToken(String token);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 根据昵称模糊查询用户列表（脱敏）
     *
     * @param userName 用户昵称
     * @param limit    最大返回数量
     * @return
     */
    List<UserVO> listUserVOByUserName(String userName, int limit);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
