package com.petcheck.server.domain.analysis.controller;

import com.petcheck.server.domain.analysis.dto.*;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // 1. [POST] 분석 생성 · OCR 시작
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisStartResponse> createAnalysis(
            @RequestPart("data") AnalysisStartRequest request,
            @RequestPart("image") MultipartFile imageFile,
            @AuthenticationPrincipal Long memberId // 🔑 시큐리티 필터가 검증한 토큰의 memberId가 그대로 들어옵니다!
    ) {
        AnalysisStartResponse response = analysisService.createAnalysis(memberId, request, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. [GET] 분석 상태 · 결과 조회
    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisDetailResponse> getAnalysisDetail(
            @PathVariable Long analysisId,
            @AuthenticationPrincipal Long memberId
    ) {
        AnalysisDetailResponse response = analysisService.getAnalysisDetail(memberId, analysisId);
        return ResponseEntity.ok(response);
    }

    // 3. [PUT] OCR 결과 수정
    @PutMapping("/{analysisId}/ocr")
    public ResponseEntity<Void> updateOcrResult(
            @PathVariable Long analysisId,
            @RequestBody OcrUpdateRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        analysisService.updateOcrResult(memberId, analysisId, request);
        return ResponseEntity.ok().build();
    }

    // 4. [POST] 분석 확정 · AI 분석 시작
    @PostMapping("/{analysisId}/confirm")
    public ResponseEntity<AnalysisStatus> confirmAndStartAi(
            @PathVariable Long analysisId,
            @AuthenticationPrincipal Long memberId
    ) {
        AnalysisStatus status = analysisService.confirmAndStartAi(memberId, analysisId);
        return ResponseEntity.ok(status);
    }

    // 5. [POST] 분석 재시도
    @PostMapping("/{analysisId}/retry")
    public ResponseEntity<AnalysisStatus> retryAnalysis(
            @PathVariable Long analysisId,
            @RequestBody RetryRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        AnalysisStatus status = analysisService.retryAnalysis(memberId, analysisId, request);
        return ResponseEntity.ok(status);
    }

    // 6. [DELETE] 분석 삭제
    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(
            @PathVariable Long analysisId,
            @AuthenticationPrincipal Long memberId
    ) {
        analysisService.deleteAnalysis(memberId, analysisId);
        return ResponseEntity.noContent().build();
    }
}