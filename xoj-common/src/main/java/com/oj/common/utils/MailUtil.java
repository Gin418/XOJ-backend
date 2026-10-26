package com.oj.common.utils;

import com.oj.common.common.ErrorCode;
import com.oj.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 邮件发送工具
 */
@Component
@Slf4j
public class MailUtil {

    @Resource
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Resource
    private ObjectProvider<MailProperties> mailPropertiesProvider;

    /**
     * 发送邮箱验证码
     *
     * @param email 收件邮箱
     * @param code  验证码
     */
    public void sendCaptchaMail(String email, String code) {
        // 未配置 spring.mail 的服务跳过发送（Bean 不存在）
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件服务未配置");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            MailProperties mailProperties = mailPropertiesProvider.getIfAvailable();
            if (mailProperties != null) {
                message.setFrom(mailProperties.getUsername());
            }
            message.setTo(email);
            message.setSubject("【XOJ 在线判题系统】邮箱验证码");
            message.setText(String.format("您的验证码是：%s，5 分钟内有效，请勿泄露给他人。", code));
            mailSender.send(message);
            log.info("send captcha mail success, email = {}", email);
        } catch (MailException e) {
            log.error("send captcha mail failed, email = {}", email, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败");
        }
    }
}
