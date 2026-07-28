package com.petcheck.server.domain.chat.dto;

import com.petcheck.server.domain.analysis.dto.IngredientMatchStatus;
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
public class ChatSource {
    private String ocrIngredient;
    private String ingredientName;
    private IngredientMatchStatus matchStatus;
    private String matchedAvoidIngredientName;
    private String description;
    private Double similarityScore;
}
