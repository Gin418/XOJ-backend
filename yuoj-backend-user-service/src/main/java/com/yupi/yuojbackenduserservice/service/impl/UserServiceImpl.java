package com.yupi.yuojbackenduserservice.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.constant.CommonConstant;
import com.yupi.yuojbackendcommon.enums.SmsEnum;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.JwtUtil;
import com.yupi.yuojbackendcommon.utils.LoginUserUtil;
import com.yupi.yuojbackendcommon.utils.MailUtil;
import com.yupi.yuojbackendcommon.utils.SmsUtil;
import com.yupi.yuojbackendcommon.utils.SqlUtils;
import com.yupi.yuojbackendmodel.model.dto.user.UserQueryRequest;
import com.yupi.yuojbackendmodel.model.entity.User;
import com.yupi.yuojbackendmodel.model.enums.UserRoleEnum;
import com.yupi.yuojbackendmodel.model.vo.LoginUserVO;
import com.yupi.yuojbackendmodel.model.vo.UserVO;
import com.yupi.yuojbackenduserservice.mapper.UserMapper;

import com.yupi.yuojbackenduserservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.yuojbackendcommon.constant.UserConstant.USER_CAPTCHA_EXPIRE_SECONDS;
import static com.yupi.yuojbackendcommon.constant.UserConstant.USER_CAPTCHA_KEY;
import static com.yupi.yuojbackendcommon.constant.UserConstant.USER_EMAIL_CAPTCHA_KEY;
import static com.yupi.yuojbackendcommon.constant.UserConstant.USER_LOGIN_EXPIRE_SECONDS;
import static com.yupi.yuojbackendcommon.constant.UserConstant.USER_LOGIN_TOKEN_KEY;


/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Gson GSON = new Gson();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MailUtil mailUtil;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String email, String captcha) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, email, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 邮箱格式校验
        ThrowUtils.throwIf(!Validator.isEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        // 校验邮箱验证码（一次性使用，校验通过即删除）
        String captchaKey = USER_EMAIL_CAPTCHA_KEY + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(captchaKey);
        ThrowUtils.throwIf(StringUtils.isBlank(cachedCode), ErrorCode.OPERATION_ERROR, "验证码已过期，请重新获取");
        ThrowUtils.throwIf(!cachedCode.equals(captcha), ErrorCode.OPERATION_ERROR, "验证码错误");
        stringRedisTemplate.delete(captchaKey);
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 邮箱不能重复
            QueryWrapper<User> emailQueryWrapper = new QueryWrapper<>();
            emailQueryWrapper.eq("email", email);
            long emailCount = this.baseMapper.selectCount(emailQueryWrapper);
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserName(userAccount);
            user.setUserPassword(encryptPassword);
            user.setEmail(email);
            user.setUserAvatar("https://xoj-914.oss-cn-hangzhou.aliyuncs.com/user/9e4a5321-156b-42ec-9484-695d8927e226.jpg");
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在（支持账号或邮箱登录）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (Validator.isEmail(userAccount)) {
            queryWrapper.eq("email", userAccount);
        } else {
            queryWrapper.eq("userAccount", userAccount);
        }
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 生成登录 token，并将登录态存入 Redis
        return doLogin(user);
    }

    @Override
    public boolean sendCaptcha(String phone) {
        // 校验手机号
        ThrowUtils.throwIf(StringUtils.isBlank(phone) || !PHONE_PATTERN.matcher(phone).matches(),
                ErrorCode.PARAMS_ERROR, "手机号格式错误");
        // 生成 6 位随机验证码并存入 Redis（5 分钟有效）
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(USER_CAPTCHA_KEY + phone, code,
                USER_CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);
        // 调用阿里云短信服务发送验证码
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        SmsUtil.sendSms(phone, SmsEnum.SEND_CAPTCHA_CODE, params);
        return true;
    }

    @Override
    public boolean sendEmailCaptcha(String email) {
        // 校验邮箱格式
        ThrowUtils.throwIf(StringUtils.isBlank(email) || !Validator.isEmail(email),
                ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        // 生成 6 位随机验证码并存入 Redis（5 分钟有效）
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(USER_EMAIL_CAPTCHA_KEY + email, code,
                USER_CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);
        // 发送验证码邮件
        mailUtil.sendCaptchaMail(email, code);
        return true;
    }

    @Override
    public LoginUserVO userSmsLogin(String phone, String code, HttpServletRequest request) {
        // 1. 校验
        ThrowUtils.throwIf(StringUtils.isBlank(phone) || !PHONE_PATTERN.matcher(phone).matches(),
                ErrorCode.PARAMS_ERROR, "手机号格式错误");
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码为空");
        // 2. 校验验证码（一次性使用，校验通过即删除）
        String captchaKey = USER_CAPTCHA_KEY + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(captchaKey);
        ThrowUtils.throwIf(StringUtils.isBlank(cachedCode), ErrorCode.OPERATION_ERROR, "验证码已过期，请重新获取");
        ThrowUtils.throwIf(!cachedCode.equals(code), ErrorCode.OPERATION_ERROR, "验证码错误");
        stringRedisTemplate.delete(captchaKey);
        // 3. 按手机号查询用户，不存在则自动注册
        User user;
        synchronized (phone.intern()) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("phone", phone);
            user = this.baseMapper.selectOne(queryWrapper);
            if (user == null) {
                user = new User();
                user.setUserAccount(phone);
//                user.setPhone(phone);
                // 验证码登录用户未设置密码，随机生成一个
                user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + RandomUtil.randomString(16)).getBytes()));
                boolean saveResult = this.save(user);
                ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
        }
        // 4. 生成登录 token，并将登录态存入 Redis
        return doLogin(user);
    }

    @Override
    public boolean userResetPassword(String email, String captcha, String newPassword, String checkPassword) {
        // 1. 校验
        ThrowUtils.throwIf(StringUtils.isAnyBlank(email, captcha, newPassword, checkPassword),
                ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(!Validator.isEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        ThrowUtils.throwIf(newPassword.length() < 8 || checkPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "新密码过短");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        // 2. 校验邮箱验证码（一次性使用，校验通过即删除）
        String captchaKey = USER_EMAIL_CAPTCHA_KEY + email;
        String cachedCode = stringRedisTemplate.opsForValue().get(captchaKey);
        ThrowUtils.throwIf(StringUtils.isBlank(cachedCode), ErrorCode.OPERATION_ERROR, "验证码已过期，请重新获取");
        ThrowUtils.throwIf(!cachedCode.equals(captcha), ErrorCode.OPERATION_ERROR, "验证码错误");
        stringRedisTemplate.delete(captchaKey);
        // 3. 查询邮箱对应的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
        // 4. 加密新密码并更新
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setUserPassword(encryptPassword);
        boolean result = this.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "密码重置失败，数据库错误");
        return true;
    }

    /**
     * 登录成功后的公共处理：生成 token、写入 Redis、返回脱敏登录用户信息
     *
     * @param user 登录用户
     * @return 脱敏后的用户信息（含 token）
     */
    private LoginUserVO doLogin(User user) {
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        String userRole = StringUtils.defaultIfBlank(user.getUserRole(), UserRoleEnum.USER.getValue());
        String token = JwtUtil.generateToken(user.getId(), userRole);
        stringRedisTemplate.opsForValue().set(USER_LOGIN_TOKEN_KEY + token, GSON.toJson(user),
                USER_LOGIN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        loginUserVO.setToken(token);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从请求头取 token，再从 Redis 中读取登录态
        User currentUser = getLoginUserByToken(getTokenFromRequest(request));
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走 Redis 缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 从请求头取 token，再从 Redis 中读取登录态
        User currentUser = getLoginUserByToken(getTokenFromRequest(request));
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走 Redis 缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User user = getLoginUserByToken(getTokenFromRequest(request));
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (getLoginUserByToken(token) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 删除 Redis 中的登录态
        stringRedisTemplate.delete(USER_LOGIN_TOKEN_KEY + token);
        return true;
    }

    @Override
    public User getLoginUserByToken(String token) {
        return LoginUserUtil.getLoginUser(stringRedisTemplate, token, User.class);
    }

    /**
     * 从请求头中获取 token（支持 Authorization: Bearer xxx）
     *
     * @param request
     * @return
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        return LoginUserUtil.getTokenFromRequest(request);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
