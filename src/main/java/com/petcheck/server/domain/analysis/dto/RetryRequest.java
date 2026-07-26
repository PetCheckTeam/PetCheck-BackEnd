package com.petcheck.server.domain.analysis.dto;

import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RetryRequest {
    private AnalysisStatus retryStep; // OCR_PROCESSING 또는 AI_ANALYZING
}