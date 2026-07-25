package com.petcheck.server.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private MemberResponse member;

    public static LoginResponse of(String accessToken, MemberResponse member) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .member(member)
                .build();
    }
}