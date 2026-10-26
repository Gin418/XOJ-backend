package com.oj.judgeservice.judge.codesandbox.impl;

import com.oj.judgeservice.judge.codesandbox.CodeSandbox;
import com.oj.model.codesandbox.ExecuteCodeRequest;
import com.oj.model.codesandbox.ExecuteCodeResponse;

/**
 * 第三方代码沙箱（调用网上现成的代码沙箱）
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱");
        return null;
    }
}
