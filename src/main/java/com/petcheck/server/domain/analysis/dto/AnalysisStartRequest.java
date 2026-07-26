package com.petcheck.server.domain.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnalysisStartRequest {
    private Long petId;
    private String productName;
}