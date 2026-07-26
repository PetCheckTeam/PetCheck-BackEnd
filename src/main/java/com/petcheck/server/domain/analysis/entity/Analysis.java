package com.petcheck.server.domain.analysis.entity;

import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, length = 500)
    private String imageUrl; // NCP Object Storage에 업로드된 이미지 URL

    @Column(columnDefinition = "TEXT")
    private String ocrRawResult; // OCR 추출 원본 데이터 (JSON or Text)

    @Column(columnDefinition = "TEXT")
    private String ocrEditedResult; // 유저가 수정/확정한 성분 목록 데이터

    @Column(columnDefinition = "TEXT")
    private String aiAnalysisResult; // AI 최종 분석 결과

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalysisStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Builder
    public Analysis(Member member, Pet pet, String productName, String imageUrl, String ocrRawResult, AnalysisStatus status) {
        this.member = member;
        this.pet = pet;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.ocrRawResult = ocrRawResult;
        this.ocrEditedResult = ocrRawResult; // 초기 수정본은 OCR 원본과 동일하게 설정
        this.status = status != null ? status : AnalysisStatus.OCR_PROCESSING;
    }

    // OCR 결과 업데이트 (OCR 완료 시)
    public void completeOcr(String ocrRawResult) {
        this.ocrRawResult = ocrRawResult;
        this.ocrEditedResult = ocrRawResult;
        this.status = AnalysisStatus.OCR_COMPLETED;
    }

    // [API 3] OCR 결과 수정 (유저 성분 수정)
    public void updateOcrResult(String editedResult) {
        this.ocrEditedResult = editedResult;
    }

    // [API 4] 분석 확정 / AI 분석 시작
    public void startAiAnalysis() {
        this.status = AnalysisStatus.AI_ANALYZING;
    }

    // AI 분석 완료 시
    public void completeAiAnalysis(String aiResult) {
        this.aiAnalysisResult = aiResult;
        this.status = AnalysisStatus.COMPLETED;
    }

    // [API 5] 분석 재시도
    public void retry(AnalysisStatus retryStatus) {
        this.status = retryStatus;
    }
}