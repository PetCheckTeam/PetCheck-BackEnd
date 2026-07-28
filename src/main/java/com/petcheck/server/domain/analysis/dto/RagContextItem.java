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
public class RagContextItem {
    private String ocrIngredient;
    private String ingredientName;
    private String safetyLevel;
    private String description;
    private Double similarityScore;
}
