package com.petcheck.server.domain.analysis.entity;

public enum AnalysisStatus {
    OCR_PROCESSING,  // OCR 분석 진행 중
    OCR_COMPLETED,   // OCR 완료 (유저 확인 및 성분 수정 대기 중)
    AI_ANALYZING,    // AI 최종 분석 진행 중
    COMPLETED,       // AI 분석 완료
    FAILED           // 처리 실패 (재시도 대상)
}