package com.petcheck.server.domain.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
public class NcpClovaOcrServiceImpl implements OcrService {

    @Value("${ncp.ocr.invoke-url}")
    private String invokeUrl;

    @Value("${ncp.ocr.secret-key}")
    private String secretKey;

    @Override
    public String extractTextFromImage(String imageUrl) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 1. HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secretKey);

            // 2. 확장자 추출 및 포맷 정규화
            String ext = imageUrl.substring(imageUrl.lastIndexOf(".") + 1);
            if (ext.contains("?")) {
                ext = ext.substring(0, ext.indexOf("?"));
            }
            ext = ext.toLowerCase();
            if ("jpeg".equals(ext)) {
                ext = "jpg";
            }

            // 3. Request Body 생성
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("version", "V2");
            jsonObject.put("requestId", UUID.randomUUID().toString());
            jsonObject.put("timestamp", System.currentTimeMillis());

            JSONArray images = new JSONArray();
            JSONObject image = new JSONObject();
            image.put("format", ext);
            image.put("name", "pet_food_label");
            image.put("url", imageUrl);

            images.put(image);
            jsonObject.put("images", images);

            HttpEntity<String> request = new HttpEntity<>(jsonObject.toString(), headers);

            // 4. API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, request, String.class);

            return parseOcrResponse(response.getBody());

        } catch (Exception e) {
            log.error("CLOVA OCR API 호출 실패 - imageUrl: {}", imageUrl, e);
            throw new RuntimeException("CLOVA OCR 처리 중 오류가 발생했습니다.", e);
        }
    }

    private String parseOcrResponse(String responseBody) {
        // 기존 작성하신 JSON 파싱 및 텍스트 추출 로직 유지
        // ...
        return responseBody;
    }
}