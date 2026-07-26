package com.petcheck.server.domain.analysis.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NcpImageUploadServiceImpl implements ImageUploadService {

    private final AmazonS3 amazonS3;

    @Value("${ncp.storage.bucket}")
    private String bucket;

    @Value("${ncp.storage.endpoint}")
    private String endPoint;

    @Override
    public String uploadImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String storeFileName = UUID.randomUUID() + "_" + originalFilename;

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            // Object Storage에 업로드 및 퍼블릭 읽기 권한 설정
            amazonS3.putObject(new PutObjectRequest(bucket, storeFileName, inputStream, objectMetadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            return endPoint + "/" + bucket + "/" + storeFileName;
        } catch (IOException e) {
            throw new RuntimeException("NCP Object Storage 이미지 업로드에 실패했습니다.", e);
        }
    }
}