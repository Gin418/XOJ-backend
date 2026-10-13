package com.yupi.yuojbackendquestionservice.controller;

import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendquestionservice.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 文件上传控制器
 *
 * @Author Q
 * @Description 接收前端上传的 zip 压缩包并解压到指定文件夹
 * @Date 2026/9/27 0:53
 * @Version 1.0.0
 **/
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private FileService fileService;

    /**
     * 接收前端上传的 zip 压缩包，解压后返回解压目录的相对路径
     *
     * @param file 前端 multipart 上传的 zip 文件（字段名为 file）
     */
    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam("file") MultipartFile file) {
        return ResultUtils.success(fileService.uploadZipFile(file));
    }
}
