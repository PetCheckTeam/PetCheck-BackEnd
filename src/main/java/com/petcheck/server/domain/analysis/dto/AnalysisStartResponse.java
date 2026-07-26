package com.petcheck.server.domain.analysis.dto;

import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisStartResponse {
    private Long analysisId;
    private AnalysisStatus status;
}