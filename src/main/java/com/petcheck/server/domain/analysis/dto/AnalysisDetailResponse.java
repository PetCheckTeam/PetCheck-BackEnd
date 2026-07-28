package com.petcheck.server.domain.analysis.dto;

import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisDetailResponse {
    private Long analysisId;
    private Long petId;
    private String productName;
    private String imageUrl;
    private AnalysisStatus status;
    private String ocrResult;       // 유저가 수정한 성분 목록 또는 OCR 원본
    private String aiAnalysisResult; // AI 최종 분석 결과 리포트

    public static AnalysisDetailResponse from(Analysis analysis) {
        return AnalysisDetailResponse.builder()
                .analysisId(analysis.getId())
                .petId(analysis.getPet().getId())
                .productName(analysis.getProductName())
                .imageUrl(analysis.getImageUrl())
                .status(analysis.getStatus())
                .ocrResult(analysis.getOcrEditedResult() != null ? analysis.getOcrEditedResult() : analysis.getOcrRawResult())
                .aiAnalysisResult(analysis.getAiAnalysisResult())
                .build();
    }
}