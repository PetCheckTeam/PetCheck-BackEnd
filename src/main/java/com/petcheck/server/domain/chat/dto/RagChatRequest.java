package com.petcheck.server.domain.chat.dto;

import com.petcheck.server.domain.analysis.dto.IngredientMatchResult;
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
public class RagChatRequest {
    private Long analysisId;
    private String productName;
    private String petName;
    private String petType;
    private List<String> avoidIngredients;
    private String ocrText;
    private List<IngredientMatchResult> ingredientResults;
    private String question;
    private List<ChatHistoryMessage> history;
}
