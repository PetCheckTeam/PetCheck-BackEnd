package com.petcheck.server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchRequest {
    private Long analysisId;
    private String ocrText;
    private Integer topK;
    private String petType;
}
