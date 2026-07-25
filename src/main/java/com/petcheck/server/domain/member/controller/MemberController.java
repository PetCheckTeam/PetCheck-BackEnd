package com.petcheck.server.domain.member.controller;

import com.petcheck.server.domain.member.dto.MemberResponse;
import com.petcheck.server.domain.member.dto.MemberUpdateRequest;
import com.petcheck.server.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users") // 👈 엔드포인트: /api/v1/users
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 1. 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        MemberResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(response);
    }

    // 2. 내 정보 수정 (닉네임 수정)
    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMyInfo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberUpdateRequest request) {
        MemberResponse response = memberService.updateMyInfo(memberId, request);
        return ResponseEntity.ok(response);
    }

    // 3. 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal Long memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build(); // 204 No Content 또는 200 OK
    }
}