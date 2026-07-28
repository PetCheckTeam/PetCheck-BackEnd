package com.petcheck.server.domain.chat.dto;

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
public class ChatTokenUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public static ChatTokenUsage empty() {
        return new ChatTokenUsage(0, 0, 0);
    }
}
