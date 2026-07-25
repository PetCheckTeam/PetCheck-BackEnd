package com.petcheck.server.domain.member.service;

import com.petcheck.server.domain.member.dto.LoginRequest;
import com.petcheck.server.domain.member.dto.LoginResponse;
import com.petcheck.server.domain.member.dto.MemberResponse;
import com.petcheck.server.domain.member.dto.SignupRequest;
import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;

    // 1. 회원가입
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // Member 엔티티 생성 및 저장 (비밀번호는 우선 평문 저장)
        Member member = Member.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .build();

        Member savedMember = memberRepository.save(member);
        return MemberResponse.from(savedMember);
    }

    // 2. 로그인
    public LoginResponse login(LoginRequest request) {
        // 회원 존재 확인
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 확인 (단순 문자열 비교)
        if (!member.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 토큰 대신 임시 토큰 값("dummy-token") 전달
        String dummyToken = "dummy-jwt-token-for-dev";

        return LoginResponse.of(dummyToken, MemberResponse.from(member));
    }

    // 3. 로그아웃
    public void logout() {
        // 현재는 별도 처리 없이 성공 응답만 반환
    }

    // 4. 로그인 상태 확인 (내 정보 조회)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return MemberResponse.from(member);
    }
}