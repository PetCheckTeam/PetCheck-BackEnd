package com.petcheck.server.domain.chat.dto;

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
public class RagPetChatRequest {
    private Long petId;
    private String petName;
    private String petType;
    private List<String> avoidIngredients;
    private String question;
    private List<ChatHistoryMessage> history;
}
