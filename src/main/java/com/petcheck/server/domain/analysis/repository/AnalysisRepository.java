package com.petcheck.server.domain.analysis.repository;

import com.petcheck.server.domain.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // 특정 회원 소유의 분석 건인지 함께 조회 (보안 검증용)
    Optional<Analysis> findByIdAndMemberId(Long id, Long memberId);
}