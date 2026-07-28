package com.petcheck.server.domain.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ChatRequest {

    @NotBlank(message = "질문은 공백일 수 없습니다.")
    @Size(max = 2000, message = "질문은 2000자 이하여야 합니다.")
    private String message;

    @Size(max = 10, message = "대화 내역은 최대 10개까지 전달할 수 있습니다.")
    @Builder.Default
    private List<@Valid ChatHistoryMessage> history = List.of();
}
