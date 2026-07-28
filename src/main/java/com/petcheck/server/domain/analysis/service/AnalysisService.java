package com.petcheck.server.domain.analysis.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.dto.*;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;
    private final PetRepository petRepository;
    private final ImageUploadService imageUploadService;
    private final OcrService ocrService;
    private final RagClient ragClient;
    private final ObjectMapper objectMapper;

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
        analysis.startAiAnalysis();

        String ocrText = analysis.getOcrEditedResult();
        if (ocrText == null || ocrText.isBlank()) {
            analysis.retry(AnalysisStatus.FAILED);
            log.warn("RAG 분석 실패 - analysisId: {}, 원인: OCR 텍스트가 비어 있습니다.", analysisId);
            return analysis.getStatus();
        }

        try {
            String petType = analysis.getPet().getSpecies();
            int topK = 1;
            logRagRequest(analysisId, petType, topK, ocrText);

            RagSearchRequest request = RagSearchRequest.builder()
                    .analysisId(analysisId)
                    .ocrText(ocrText)
                    .topK(topK)
                    .petType(petType)
                    .build();

            RagSearchResponse response = ragClient.search(request);
            String ragResultJson = objectMapper.writeValueAsString(response);
            analysis.completeAiAnalysis(ragResultJson);
        } catch (Exception error) {
            analysis.retry(AnalysisStatus.FAILED);
            log.error(
                    "RAG 분석 실패 - analysisId: {}, 오류 유형: {}, 원인: {}",
                    analysisId,
                    error.getClass().getSimpleName(),
                    error.getMessage()
            );
        }

        return analysis.getStatus();
    }

    private void logRagRequest(Long analysisId, String petType, int topK, String ocrText) {
        String singleLineText = ocrText.replaceAll("\\R", " ");
        String preview = singleLineText.length() <= 500
                ? singleLineText
                : singleLineText.substring(0, 500);

        log.info(
                "RAG 요청 데이터 - analysisId: {}, petType: {}, topK: {}, "
                        + "ocrTextLength: {}, ocrTextPreview: [{}]",
                analysisId,
                petType,
                topK,
                ocrText.length(),
                preview
        );
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
