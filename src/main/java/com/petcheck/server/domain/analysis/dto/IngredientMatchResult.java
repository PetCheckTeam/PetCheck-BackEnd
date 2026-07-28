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
public class IngredientMatchResult {
    private String ocrIngredient;
    private String ingredientName;
    private IngredientMatchStatus matchStatus;
    // PetAvoidIngredient 등록 API에서 사용하는 표준 Ingredient.id
    private Long matchedAvoidIngredientId;
    private String matchedAvoidIngredientName;
    private String description;
    private Double similarityScore;
}
