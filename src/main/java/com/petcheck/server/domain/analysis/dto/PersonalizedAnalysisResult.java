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
public class PersonalizedAnalysisResult {
    private Long analysisId;
    private int totalIngredientCount;
    private int matchedCount;
    @Builder.Default
    private List<IngredientMatchResult> matchedIngredients = List.of();
    @Builder.Default
    private List<IngredientMatchResult> ingredientResults = List.of();
}
