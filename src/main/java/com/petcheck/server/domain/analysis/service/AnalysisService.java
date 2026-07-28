package com.petcheck.server.domain.analysis.service;

import com.petcheck.server.domain.analysis.dto.*;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;
    private final PetRepository petRepository;
    private final ImageUploadService imageUploadService;
    private final OcrService ocrService;

    // 1. [POST /api/v1/analyses] 분석 생성 및 OCR 시작
    @Transactional
    public AnalysisStartResponse createAnalysis(Long memberId, AnalysisStartRequest request, MultipartFile imageFile) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));

        // Step A: NCP Object Storage에 이미지 업로드
        String imageUrl = imageUploadService.uploadImage(imageFile);

        // Step B: Analysis 엔티티 생성 (초기 상태: OCR_PROCESSING)
        Analysis analysis = Analysis.builder()
                .member(member)
                .pet(pet)
                .productName(request.getProductName())
                .imageUrl(imageUrl)
                .status(AnalysisStatus.OCR_PROCESSING)
                .build();

        analysisRepository.save(analysis);

        // Step C: NCP CLOVA OCR 연동
        try {
            String extractedText = ocrService.extractTextFromImage(imageUrl);
            analysis.completeOcr(extractedText); // OCR 완료 상태로 변경
        } catch (Exception e) {
            analysis.retry(AnalysisStatus.FAILED);
        }

        return new AnalysisStartResponse(analysis.getId(), analysis.getStatus());
    }

    // 2. [GET /api/v1/analyses/{analysisId}] 분석 상태·결과 조회
    public AnalysisDetailResponse getAnalysisDetail(Long memberId, Long analysisId) {
        Analysis analysis = findAnalysisWithAuth(analysisId, memberId);
        return AnalysisDetailResponse.from(analysis);
    }

    // 3. [PUT /api/v1/analyses/{analysisId}/ocr] OCR 결과 수정
    @Transactional
    public void updateOcrResult(Long memberId, Long analysisId, OcrUpdateRequest request) {
        Analysis analysis = findAnalysisWithAuth(analysisId, memberId);
        analysis.updateOcrResult(request.getEditedOcrResult());
    }

    // 4. [POST /api/v1/analyses/{analysisId}/confirm] 분석 확정 및 AI 분석 시작
    @Transactional
    public AnalysisStatus confirmAndStartAi(Long memberId, Long analysisId) {
        Analysis analysis = findAnalysisWithAuth(analysisId, memberId);

        // AI 분석 상태로 전환
        analysis.startAiAnalysis();

        // TODO: (추후 4단계) HyperCLOVA X 등 AI 분석 호출 및 결과 저장 로직 연동 위치
        // 현재는 흐름 동작을 위해 완성 처리 예시 코드
        String mockAiResponse = "해당 사료는 " + analysis.getPet().getName() + "의 연령과 영양 균형에 적합합니다.";
        analysis.completeAiAnalysis(mockAiResponse);

        return analysis.getStatus();
    }

    // 5. [POST /api/v1/analyses/{analysisId}/retry] 분석 재시도
    @Transactional
    public AnalysisStatus retryAnalysis(Long memberId, Long analysisId, RetryRequest request) {
        Analysis analysis = findAnalysisWithAuth(analysisId, memberId);

        AnalysisStatus retryStep = request.getRetryStep();
        if (retryStep == AnalysisStatus.OCR_PROCESSING) {
            try {
                analysis.retry(AnalysisStatus.OCR_PROCESSING);
                String extractedText = ocrService.extractTextFromImage(analysis.getImageUrl());
                analysis.completeOcr(extractedText);
            } catch (Exception e) {
                analysis.retry(AnalysisStatus.FAILED);
            }
        } else if (retryStep == AnalysisStatus.AI_ANALYZING) {
            analysis.startAiAnalysis();
            // AI 분석 재시도 로직
        }

        return analysis.getStatus();
    }

    // 6. [DELETE /api/v1/analyses/{analysisId}] 분석 삭제
    @Transactional
    public void deleteAnalysis(Long memberId, Long analysisId) {
        Analysis analysis = findAnalysisWithAuth(analysisId, memberId);
        analysisRepository.delete(analysis);
    }

    // 접근 권한 검증 공통 메서드
    private Analysis findAnalysisWithAuth(Long analysisId, Long memberId) {
        return analysisRepository.findByIdAndMemberId(analysisId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("분석 내역을 찾을 수 없거나 접근 권한이 없습니다."));
    }
}