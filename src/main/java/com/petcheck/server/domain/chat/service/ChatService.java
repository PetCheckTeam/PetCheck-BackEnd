package com.petcheck.server.domain.chat.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.dto.IngredientMatchResult;
import com.petcheck.server.domain.analysis.dto.PersonalizedAnalysisResult;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.chat.dto.ChatHistoryMessage;
import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.RagChatRequest;
import com.petcheck.server.domain.chat.dto.RagChatResponse;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import com.petcheck.server.domain.pet.repository.PetAvoidIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int MAX_HISTORY_SIZE = 10;

    private final AnalysisRepository analysisRepository;
    private final PetAvoidIngredientRepository petAvoidIngredientRepository;
    private final RagClient ragClient;
    private final ObjectMapper objectMapper;

    public ChatResponse chat(Long memberId, Long analysisId, ChatRequest request) {
        Analysis analysis = analysisRepository.findByIdAndMemberId(analysisId, memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "분석 내역을 찾을 수 없거나 접근 권한이 없습니다."
                ));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new IllegalStateException("완료된 분석 결과에만 질문할 수 있습니다.");
        }
        if (analysis.getAiAnalysisResult() == null || analysis.getAiAnalysisResult().isBlank()) {
            throw new IllegalStateException("질문에 사용할 분석 결과가 없습니다.");
        }

        PersonalizedAnalysisResult personalizedResult = deserializeAnalysisResult(analysis);
        Pet pet = analysis.getPet();
        List<String> avoidIngredients = petAvoidIngredientRepository
                .findAllByPetIdAndPetMemberId(pet.getId(), memberId)
                .stream()
                .map(PetAvoidIngredient::getIngredient)
                .filter(ingredient -> ingredient != null && ingredient.getStandardName() != null)
                .map(ingredient -> ingredient.getStandardName())
                .toList();

        RagChatRequest ragRequest = RagChatRequest.builder()
                .analysisId(analysis.getId())
                .productName(analysis.getProductName())
                .petName(pet.getName())
                .petType(pet.getSpecies())
                .avoidIngredients(avoidIngredients)
                .ocrText(analysis.getOcrEditedResult())
                .ingredientResults(safeIngredientResults(personalizedResult))
                .question(request.getMessage())
                .history(recentHistory(request.getHistory()))
                .build();

        RagChatResponse ragResponse = ragClient.chat(ragRequest);
        return ChatResponse.from(ragResponse);
    }

    private PersonalizedAnalysisResult deserializeAnalysisResult(Analysis analysis) {
        try {
            PersonalizedAnalysisResult result = objectMapper.readValue(
                    analysis.getAiAnalysisResult(),
                    PersonalizedAnalysisResult.class
            );
            if (result == null) {
                throw new IllegalStateException("저장된 분석 결과가 비어 있습니다.");
            }
            return result;
        } catch (Exception error) {
            if (error instanceof IllegalStateException stateError) {
                throw stateError;
            }
            throw new IllegalStateException("저장된 분석 결과를 읽을 수 없습니다.", error);
        }
    }

    private List<IngredientMatchResult> safeIngredientResults(
            PersonalizedAnalysisResult personalizedResult
    ) {
        return personalizedResult.getIngredientResults() != null
                ? personalizedResult.getIngredientResults()
                : List.of();
    }

    private List<ChatHistoryMessage> recentHistory(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_SIZE);
        return List.copyOf(history.subList(fromIndex, history.size()));
    }
}
