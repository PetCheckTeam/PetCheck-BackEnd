package com.petcheck.server.domain.member.service;

import com.petcheck.server.domain.member.dto.LoginRequest;
import com.petcheck.server.domain.member.dto.LoginResponse;
import com.petcheck.server.domain.member.dto.MemberResponse;
import com.petcheck.server.domain.member.dto.SignupRequest;
import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.global.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // 👈 비밀번호 암호화/검증용
    private final JwtProvider jwtProvider;         // 👈 JWT 토큰 생성용

    // 1. 회원가입
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화 후 Member 엔티티 생성 및 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member member = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
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

        // 비밀번호 암호화 비교 (passwordEncoder.matches 사용)
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 진짜 JWT Access Token 생성!
        String accessToken = jwtProvider.createToken(member.getId(), member.getEmail());

        return LoginResponse.of(accessToken, MemberResponse.from(member));
    }

    // 3. 로그아웃
    public void logout() {
        // Client 측에서 저장된 Token을 삭제하는 방식(Stateless)을 사용할 경우 backend 비즈니스 로직은 비워둡니다.
        // (추후 Redis 등을 도입해 Blacklist 처리를 할 때 이곳에 로직을 추가합니다.)
    }

    // 4. 로그인 상태 확인 (내 정보 조회)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return MemberResponse.from(member);
    }
}