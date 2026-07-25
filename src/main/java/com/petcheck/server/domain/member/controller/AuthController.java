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
        authService.logout();
        return ResponseEntity.ok().build();
    }

    // 4. 로그인 상태 확인 (내 정보 조회)
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal Long memberId) {
        // JwtAuthenticationFilter에서 저장한 인증 정보(memberId)를 직접 인자로 받습니다.
        MemberResponse response = authService.getMyInfo(memberId);
        return ResponseEntity.ok(response);
    }
}