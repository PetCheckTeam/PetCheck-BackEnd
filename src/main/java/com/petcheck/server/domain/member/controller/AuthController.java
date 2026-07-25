package com.petcheck.server.domain.member.controller;

import com.petcheck.server.domain.member.dto.LoginRequest;
import com.petcheck.server.domain.member.dto.LoginResponse;
import com.petcheck.server.domain.member.dto.MemberResponse;
import com.petcheck.server.domain.member.dto.SignupRequest;
import com.petcheck.server.domain.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. 회원가입
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        MemberResponse response = authService.signup(request);
        return ResponseEntity.ok(response);
    }

    // 2. 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // 3. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT 기반 인증에서는 서버 세션을 비우거나 필요한 경우 토큰 블랙리스트 처리를 수행합니다.
        authService.logout();
        return ResponseEntity.ok().build();
    }

    // 4. 로그인 상태 확인 (현재 사용자 정보 조회)
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        // SecurityContext에 저장된 회원 ID를 바탕으로 회원 정보를 반환합니다.
        MemberResponse response = authService.getMyInfo(memberId);
        return ResponseEntity.ok(response);
    }
}