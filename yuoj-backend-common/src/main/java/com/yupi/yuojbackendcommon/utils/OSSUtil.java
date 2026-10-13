package com.yupi.yuojbackendcommon.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.yupi.yuojbackendcommon.enums.OSSEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.UUID;

/**
 * @Author Q
 * @Description OSS 文件上传下载工具，访问凭证等配置从 application.yml 的 oss.* 读取
 * @Date 2026/9/28 16:51
 * @Version 1.0.0
 **/
@Component
public class OSSUtil {

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.region}")
    private String region;

    private static OSS ossClient;

    @PostConstruct
    public void init() {
        // 创建凭证提供者
        DefaultCredentialProvider provider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 配置客户端参数
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用V4签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(provider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();
    }


    public static String uploadFile(MultipartFile file, OSSEnum ossEnum, String objectName) throws Exception {
        try {
            String name = file.getOriginalFilename();
            String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
            objectName = objectName + "/" + UUID.randomUUID() + "." + ext;
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossEnum.bucketName, objectName, new ByteArrayInputStream(file.getBytes()));
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            putObjectResult.getVersionId();
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
            throw new RuntimeException("上传文件到OSS失败: " + oe.getErrorCode() + " " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
            throw new RuntimeException("上传文件到OSS失败(网络异常)", ce);
        }
        return ossEnum.rawUrl + objectName;
    }

    public static void downloadFile(String objectName, OSSEnum ossEnum, String filePath) {
        try {
            ossClient.getObject(new GetObjectRequest(ossEnum.bucketName, objectName), new File(filePath));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
            throw new RuntimeException("下载文件失败: " + oe.getErrorCode() + " " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
            throw new RuntimeException("下载文件失败(网络异常)", ce);
        }
    }
}
