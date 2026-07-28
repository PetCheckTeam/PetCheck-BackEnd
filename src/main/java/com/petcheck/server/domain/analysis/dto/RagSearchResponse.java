package com.petcheck.server.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {
    private Long analysisId;
    private List<String> extractedIngredients;
    private Integer totalCount;
    private List<RagContextItem> contexts;
}
