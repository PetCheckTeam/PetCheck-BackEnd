package com.petcheck.server.domain.chat.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.client.RagClientException;
import com.petcheck.server.domain.chat.dto.ChatHistoryMessage;
import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.RagChatResponse;
import com.petcheck.server.domain.chat.dto.RagPetChatRequest;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import com.petcheck.server.domain.pet.repository.PetAvoidIngredientRepository;
import com.petcheck.server.domain.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetChatService {

    private final PetRepository petRepository;
    private final PetAvoidIngredientRepository petAvoidIngredientRepository;
    private final RagClient ragClient;

    public ChatResponse chat(Long memberId, Long petId, ChatRequest request) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 반려동물입니다."
                ));

        if (!memberId.equals(pet.getMember().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 권한이 없는 반려동물입니다."
            );
        }

        List<String> avoidIngredients = petAvoidIngredientRepository
                .findAllByPetIdAndPetMemberId(petId, memberId)
                .stream()
                .map(PetAvoidIngredient::getIngredient)
                .filter(ingredient -> ingredient != null
                        && ingredient.getStandardName() != null
                        && !ingredient.getStandardName().isBlank())
                .map(ingredient -> ingredient.getStandardName())
                .toList();

        RagPetChatRequest ragRequest = RagPetChatRequest.builder()
                .petId(pet.getId())
                .petName(pet.getName())
                .petType(pet.getSpecies())
                .avoidIngredients(avoidIngredients)
                .question(request.getMessage())
                .history(safeHistory(request.getHistory()))
                .build();

        RagChatResponse ragResponse;
        try {
            ragResponse = ragClient.petChat(ragRequest);
        } catch (RagClientException error) {
            throw upstreamException(error);
        }

        if (ragResponse == null
                || ragResponse.getAnswer() == null
                || ragResponse.getAnswer().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "RAG 챗 서버가 유효한 답변을 반환하지 않았습니다."
            );
        }

        return ChatResponse.from(ragResponse);
    }

    private List<ChatHistoryMessage> safeHistory(List<ChatHistoryMessage> history) {
        return history == null || history.isEmpty()
                ? List.of()
                : List.copyOf(history);
    }

    private ResponseStatusException upstreamException(RagClientException error) {
        if (error.getType() == RagClientException.Type.TIMEOUT) {
            return new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "RAG 챗 서버 응답 시간이 초과되었습니다.",
                    error
            );
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "RAG 챗 서버에서 유효한 응답을 받지 못했습니다.",
                error
        );
    }
}
