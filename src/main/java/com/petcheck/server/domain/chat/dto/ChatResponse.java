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
public class ChatResponse {
    private String answer;
    private String model;
    private String finishReason;
    private ChatTokenUsage usage;
    private List<ChatSource> sources;

    public static ChatResponse from(RagChatResponse response) {
        return ChatResponse.builder()
                .answer(response.getAnswer())
                .model(response.getModel())
                .finishReason(response.getFinishReason())
                .usage(response.getUsage() != null ? response.getUsage() : ChatTokenUsage.empty())
                .sources(response.getSources() != null ? response.getSources() : List.of())
                .build();
    }
}
