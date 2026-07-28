package com.petcheck.server.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class ChatHistoryMessage {

    @NotBlank(message = "대화 역할은 필수입니다.")
    @Pattern(
            regexp = "user|assistant",
            message = "대화 역할은 user 또는 assistant만 사용할 수 있습니다."
    )
    private String role;

    @NotBlank(message = "대화 내용은 공백일 수 없습니다.")
    @Size(max = 10000, message = "대화 내용은 10000자 이하여야 합니다.")
    private String content;
}
