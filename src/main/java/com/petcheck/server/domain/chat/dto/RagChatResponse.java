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
public class RagChatResponse {
    private String answer;
    private String model;
    private String finishReason;
    private ChatTokenUsage usage;
    private List<ChatSource> sources;
}
