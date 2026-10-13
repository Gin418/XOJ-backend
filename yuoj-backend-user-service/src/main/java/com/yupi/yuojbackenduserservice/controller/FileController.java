package com.yupi.yuojbackenduserservice.controller;

import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.enums.OSSEnum;
import com.yupi.yuojbackendcommon.utils.OSSUtil;
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
