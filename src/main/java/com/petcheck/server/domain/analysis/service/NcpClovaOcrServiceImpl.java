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

    String parseOcrResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("CLOVA OCR 응답이 비어 있습니다.");
        }

        JSONObject response = new JSONObject(responseBody);
        JSONArray images = response.optJSONArray("images");
        if (images == null || images.isEmpty()) {
            throw new IllegalStateException("CLOVA OCR 응답에 images가 없습니다.");
        }

        StringBuilder extractedText = new StringBuilder();
        boolean foundText = false;

        for (int imageIndex = 0; imageIndex < images.length(); imageIndex++) {
            JSONObject image = images.optJSONObject(imageIndex);
            if (image == null) {
                continue;
            }

            JSONArray fields = image.optJSONArray("fields");
            if (fields == null) {
                continue;
            }

            for (int fieldIndex = 0; fieldIndex < fields.length(); fieldIndex++) {
                JSONObject field = fields.optJSONObject(fieldIndex);
                if (field == null) {
                    continue;
                }

                String inferText = field.optString("inferText", "").trim();
                if (inferText.isEmpty()) {
                    continue;
                }

                appendOcrField(extractedText, inferText);
                foundText = true;

                if (field.optBoolean("lineBreak", false)) {
                    appendLineBreak(extractedText);
                }
            }

            if (foundText && imageIndex < images.length() - 1) {
                appendLineBreak(extractedText);
            }
        }

        String result = extractedText.toString().strip();
        if (!foundText || result.isEmpty()) {
            throw new IllegalStateException("CLOVA OCR 응답에서 inferText를 찾을 수 없습니다.");
        }
        return result;
    }

    private void appendOcrField(StringBuilder target, String value) {
        if (target.isEmpty()) {
            target.append(value);
            return;
        }

        char previous = target.charAt(target.length() - 1);
        char current = value.charAt(0);
        boolean punctuation = ",.;:!?%)]}〉》」』、，。".indexOf(current) >= 0;
        boolean afterOpeningBracket = "([{〈《「『".indexOf(previous) >= 0;

        if (!Character.isWhitespace(previous) && !punctuation && !afterOpeningBracket) {
            target.append(' ');
        }
        target.append(value);
    }

    private void appendLineBreak(StringBuilder target) {
        while (!target.isEmpty() && target.charAt(target.length() - 1) == ' ') {
            target.deleteCharAt(target.length() - 1);
        }
        if (!target.isEmpty() && target.charAt(target.length() - 1) != '\n') {
            target.append('\n');
        }
    }
}
