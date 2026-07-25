package com.petcheck.server.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {

    @NotBlank(message = "닉네임은 필수 입력 값입니다.")
    private String nickname;

    public MemberUpdateRequest(String nickname) {
        this.nickname = nickname;
    }
}