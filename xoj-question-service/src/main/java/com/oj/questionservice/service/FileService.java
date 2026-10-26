package com.oj.questionservice.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 *
 * @Author Q
 * @Description 接收前端上传的 zip 压缩包
 * @Date 2026/9/27 0:53
 * @Version 1.0.0
 **/
public interface FileService {

    /**
     * 接收前端上传的 zip 压缩包，解压后返回阿里云地址
     *
     * @param file 前端 multipart 上传的 zip 文件
     * @return
     */
    String uploadZipFile(MultipartFile file);

    String downloadFile(String rawUrl, Long questionId);

    /**
     * 删除题目的测试点下载缓存文件（download 目录下的 zip）
     *
     * @param questionId 题目 id
     */
    void deleteQuestionFiles(Long questionId);
}
