package com.yupi.yuojbackendquestionservice.service.impl;

import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.enums.OSSEnum;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.utils.OSSUtil;
import com.yupi.yuojbackendquestionservice.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件服务实现类
 *
 * @Author Q
 * @Description 接收前端上传的 zip 压缩包并解压到指定文件夹
 * @Date 2026/9/27 0:53
 * @Version 1.0.0
 **/
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final String UPLOAD_DIR = "upload";

    private static final String DOWNLOAD_DIR = "download";

    @Override
    public String uploadZipFile(MultipartFile file) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传 zip 压缩包");
        }

        try {
            String rawUrl = OSSUtil.uploadFile(file, OSSEnum.USER, "question");
            return rawUrl.replace(OSSEnum.USER.rawUrl, "");
        } catch (Exception e) {
            log.error("上传文件到OSS失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传文件到OSS失败");
        }

    }

    @Override
    public String downloadFile(String objectName, Long questionId) {
        log.info("下载文件，objectName: {}, questionId: {}", objectName, questionId);
        String filePath = DOWNLOAD_DIR + "/" + questionId + ".zip";
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建下载目录失败");
        }
        OSSUtil.downloadFile(objectName, OSSEnum.USER, filePath);
        File zipFile = new File(filePath);
        File destDir;
        try {
            destDir = new File(UPLOAD_DIR, questionId.toString()).getCanonicalFile();
        } catch (IOException e) {
            log.error("解析解压目录失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建解压目录失败");
        }
        // 目录已存在则先清空旧文件，避免新旧测试点混杂
        if (destDir.exists()) {
            deleteDirectory(destDir);
        }
        if (!destDir.mkdirs() && !destDir.exists()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建解压目录失败");
        }
        // 优先用 UTF-8 解压；若文件名出现乱码（Windows 打包的 zip 可能为 GBK 编码）则改用 GBK 重试
        boolean garbled = unzip(zipFile, destDir, StandardCharsets.UTF_8);
        if (garbled) {
            log.info("zip 文件名出现乱码，改用 GBK 编码重新解压, file: {}", zipFile.getName());
            deleteDirectory(destDir);
            unzip(zipFile, destDir, Charset.forName("GBK"));
        }
        // 返回解压目录的相对路径（相对服务运行目录，如 upload/uuid；跨盘符时回退绝对路径）
        try {
            Path basePath = Paths.get("").toAbsolutePath().normalize();
            return basePath.relativize(destDir.toPath()).toString();
        } catch (IllegalArgumentException e) {
            log.warn("解压目录与运行目录不在同一根路径，返回绝对路径");
            return destDir.getAbsolutePath();
        }
    }

    @Override
    public void deleteQuestionFiles(Long questionId) {
        File zipFile = new File(DOWNLOAD_DIR + File.separator + questionId + ".zip");
        if (zipFile.exists() && !zipFile.delete()) {
            log.warn("删除题目测试点缓存失败: {}", zipFile.getAbsolutePath());
        }
    }

    /**
     * 将 zip 解压到指定目录
     *
     * @return 是否检测到文件名乱码
     */
    private boolean unzip(File file, File destDir, Charset charset) {
        boolean garbled = false;
        try (InputStream inputStream = new FileInputStream(file);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, charset)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.contains("\uFFFD")) {
                    garbled = true;
                }
                Path targetPath = safeResolve(destDir, entryName);
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            log.error("解压 zip 失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解压 zip 文件失败");
        }
        return garbled;
    }

    /**
     * 拼接解压目标路径，防止 zip slip 路径穿越攻击
     */
    private Path safeResolve(File destDir, String entryName) {
        try {
            String destDirCanonicalPath = destDir.getCanonicalPath();
            Path targetPath = Paths.get(destDirCanonicalPath, entryName).normalize();
            if (!targetPath.startsWith(destDirCanonicalPath)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "压缩包内存在非法路径，已拒绝解压");
            }
            return targetPath;
        } catch (IOException e) {
            log.error("解析压缩包内路径失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析压缩包内路径失败");
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
