package com.petcheck.server.domain.member.repository;

import com.petcheck.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 회원가입 시 이메일 중복 체크용
    boolean existsByEmail(String email);

    // 로그인 시 이메일로 회원 정보 조회용
    Optional<Member> findByEmail(String email);
}