package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private  AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file);
        try {
            // 获取文件的原始名称
            String orignalfilename = file.getOriginalFilename();
            // 获取文件的扩展名
            String extension = orignalfilename.substring(orignalfilename.lastIndexOf("."));
            // 使用UUID和扩展名，构造新的文件名称
            String objectName = UUID.randomUUID().toString() + extension;
            // 文件的请求路径
            String filepath = aliOssUtil.upload(file.getBytes(), objectName) ; // UUID，防止上传的文件名称有重名的
            return Result.success(filepath);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("文件上传失败：{}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }


}
