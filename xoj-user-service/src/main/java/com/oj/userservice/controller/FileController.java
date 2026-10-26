package com.oj.userservice.controller;

import com.oj.common.common.BaseResponse;
import com.oj.common.common.ResultUtils;
import com.oj.common.enums.OSSEnum;
import com.oj.common.utils.OSSUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author Q
 * @Description TODO
 * @Date 2026/9/28 16:44
 * @Version 1.0.0
 **/
@RestController
@RequestMapping("/file")
public class FileController {

    @PostMapping("/upload")
    public BaseResponse<String> upload(MultipartFile file) throws Exception {
        String rawUrl = OSSUtil.uploadFile(file, OSSEnum.USER, "user");
        return ResultUtils.success(rawUrl);
    }

}
