package com.petcheck.server.domain.analysis.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.client.RagClientException;
import com.petcheck.server.domain.analysis.dto.AnalysisDetailResponse;
import com.petcheck.server.domain.analysis.dto.IngredientMatchStatus;
import com.petcheck.server.domain.analysis.dto.PersonalizedAnalysisResult;
import com.petcheck.server.domain.analysis.dto.RagContextItem;
import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.dto.RagSearchResponse;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.ingredient.entity.Ingredient;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import com.petcheck.server.domain.pet.repository.PetAvoidIngredientRepository;
import com.petcheck.server.domain.pet.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long ANALYSIS_ID = 10L;
    private static final Long PET_ID = 5L;

    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetAvoidIngredientRepository petAvoidIngredientRepository;
    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private OcrService ocrService;
    @Mock
    private RagClient ragClient;

    private ObjectMapper objectMapper;
    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        analysisService = new AnalysisService(
                analysisRepository,
                memberRepository,
                petRepository,
                petAvoidIngredientRepository,
                imageUploadService,
                ocrService,
                ragClient,
                objectMapper
        );
    }

    @Test
    void 회피_성분과_일치한_RAG_원료만_맞춤_결과에_저장하고_반환한다() throws Exception {
        Analysis analysis = analysis("원료명: 닭고기, 계란, 옥수수");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(ragResponse(
                context("닭고기", "닭-고기", "닭 유래 원료", 1.0),
                context("계란", "계 란", "계란 유래 원료", 0.99),
                context("옥수수", "옥수수", "곡물 원료", 0.95)
        ));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(
                        avoidIngredient(1L, "닭고기"),
                        avoidIngredient(2L, "계란")
                ));

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
        PersonalizedAnalysisResult storedResult = objectMapper.readValue(
                analysis.getAiAnalysisResult(),
                PersonalizedAnalysisResult.class
        );
        assertThat(storedResult.getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(storedResult.getTotalIngredientCount()).isEqualTo(3);
        assertThat(storedResult.getMatchedCount()).isEqualTo(2);
        assertThat(storedResult.getMatchedIngredients())
                .extracting(result -> result.getMatchedAvoidIngredientName())
                .containsExactly("닭고기", "계란");
        assertThat(storedResult.getMatchedIngredients())
                .extracting(result -> result.getIngredientName())
                .doesNotContain("옥수수");
        assertThat(storedResult.getIngredientResults())
                .extracting(result -> result.getMatchStatus())
                .containsExactly(
                        IngredientMatchStatus.MATCHED,
                        IngredientMatchStatus.MATCHED,
                        IngredientMatchStatus.NOT_MATCHED
                );

        AnalysisDetailResponse detailResponse = analysisService.getAnalysisDetail(MEMBER_ID, ANALYSIS_ID);
        assertThat(detailResponse.getAiAnalysisResult().getMatchedCount()).isEqualTo(2);
        assertThat(detailResponse.getAiAnalysisResult().getMatchedIngredients())
                .hasSize(2);

        ArgumentCaptor<RagSearchRequest> requestCaptor = ArgumentCaptor.forClass(RagSearchRequest.class);
        verify(ragClient).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(requestCaptor.getValue().getOcrText()).isEqualTo("원료명: 닭고기, 계란, 옥수수");
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(1);
        assertThat(requestCaptor.getValue().getPetType()).isEqualTo("DOG");
    }

    @Test
    void 회피_성분이_없는_Pet은_matchedCount가_0이다() throws Exception {
        Analysis analysis = analysis("원료명: 닭고기");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(ragResponse(
                context("닭고기", "닭고기", "닭 유래 원료", 1.0)
        ));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of());

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        PersonalizedAnalysisResult result = storedResult(analysis);
        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getMatchedIngredients()).isEmpty();
        assertThat(result.getIngredientResults())
                .singleElement()
                .satisfies(item -> assertThat(item.getMatchStatus())
                        .isEqualTo(IngredientMatchStatus.NOT_MATCHED));
    }

    @Test
    void RAG_contexts가_비어_있으면_matchedCount가_0이다() throws Exception {
        Analysis analysis = analysis("원료명 없음");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(RagSearchResponse.builder()
                .analysisId(ANALYSIS_ID)
                .totalCount(0)
                .contexts(List.of())
                .build());
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(avoidIngredient(1L, "닭고기")));

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        PersonalizedAnalysisResult result = storedResult(analysis);
        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.getTotalIngredientCount()).isZero();
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getMatchedIngredients()).isEmpty();
        assertThat(result.getIngredientResults()).isEmpty();
    }

    @Test
    void RAG_context와_필드가_null이어도_UNKNOWN으로_처리한다() throws Exception {
        Analysis analysis = analysis("표준화되지 않은 원료");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(RagSearchResponse.builder()
                .analysisId(ANALYSIS_ID)
                .totalCount(null)
                .contexts(Arrays.asList(null, RagContextItem.builder().build()))
                .build());
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(avoidIngredient(1L, "닭고기")));

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        PersonalizedAnalysisResult result = storedResult(analysis);
        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(result.getTotalIngredientCount()).isEqualTo(2);
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getIngredientResults())
                .extracting(item -> item.getMatchStatus())
                .containsExactly(IngredientMatchStatus.UNKNOWN, IngredientMatchStatus.UNKNOWN);
    }

    @Test
    void 부분_문자열은_회피_성분으로_매칭하지_않는다() throws Exception {
        Analysis analysis = analysis("원료명: 옥수수전분");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(ragResponse(
                context("옥수수전분", "옥수수전분", null, null)
        ));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(avoidIngredient(3L, "옥수수")));

        analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        PersonalizedAnalysisResult result = storedResult(analysis);
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getIngredientResults().get(0).getMatchStatus())
                .isEqualTo(IngredientMatchStatus.NOT_MATCHED);
    }

    @Test
    void 다른_사용자의_PetAvoidIngredient를_조회하지_않는다() {
        Analysis analysis = analysis("원료명: 닭고기");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(ragResponse(
                context("닭고기", "닭고기", null, 1.0)
        ));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of());

        analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        verify(petAvoidIngredientRepository)
                .findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID);
        verify(petAvoidIngredientRepository, never())
                .findAllByPetIdAndPetMemberId(PET_ID, 8L);
    }

    @Test
    void 기존에_저장된_RAG_JSON도_구조화된_맞춤_결과로_반환한다() throws Exception {
        Analysis analysis = analysis("원료명: 닭고기, 옥수수");
        analysis.completeAiAnalysis(objectMapper.writeValueAsString(ragResponse(
                context("닭고기", "닭고기", "닭 유래 원료", 1.0),
                context("옥수수", "옥수수", "곡물 원료", 0.9)
        )));
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(avoidIngredient(1L, "닭고기")));

        AnalysisDetailResponse response = analysisService.getAnalysisDetail(MEMBER_ID, ANALYSIS_ID);

        assertThat(response.getAiAnalysisResult().getTotalIngredientCount()).isEqualTo(2);
        assertThat(response.getAiAnalysisResult().getMatchedCount()).isEqualTo(1);
        assertThat(response.getAiAnalysisResult().getMatchedIngredients())
                .singleElement()
                .satisfies(item -> assertThat(item.getMatchedAvoidIngredientName())
                        .isEqualTo("닭고기"));
        verifyNoInteractions(ragClient);
    }

    @Test
    void OCR_텍스트가_비면_FAILED로_변경한다() {
        Analysis analysis = analysis("  ");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        assertThat(status).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getAiAnalysisResult()).isNull();
        verifyNoInteractions(ragClient, petAvoidIngredientRepository);
    }

    @Test
    void RAG_실패_후_FAILED로_변경한다() {
        Analysis analysis = analysis("원료명: 닭고기");
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenThrow(new RagClientException(
                RagClientException.Type.CONNECTION,
                "RAG 서버에 연결할 수 없습니다."
        ));

        AnalysisStatus status = analysisService.confirmAndStartAi(MEMBER_ID, ANALYSIS_ID);

        assertThat(status).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getAiAnalysisResult()).isNull();
        verifyNoInteractions(petAvoidIngredientRepository);
    }

    private Analysis analysis(String ocrText) {
        Pet pet = Pet.builder()
                .name("보리")
                .species("DOG")
                .build();
        ReflectionTestUtils.setField(pet, "id", PET_ID);

        Analysis analysis = Analysis.builder()
                .pet(pet)
                .productName("샘플 사료")
                .imageUrl("https://example.com/ingredient.jpg")
                .ocrRawResult(ocrText)
                .status(AnalysisStatus.OCR_COMPLETED)
                .build();
        ReflectionTestUtils.setField(analysis, "id", ANALYSIS_ID);
        return analysis;
    }

    private PetAvoidIngredient avoidIngredient(Long ingredientId, String standardName) {
        Ingredient ingredient = Ingredient.builder()
                .standardName(standardName)
                .description(standardName + " 설명")
                .build();
        ReflectionTestUtils.setField(ingredient, "id", ingredientId);
        return PetAvoidIngredient.builder()
                .ingredient(ingredient)
                .build();
    }

    private RagSearchResponse ragResponse(RagContextItem... contexts) {
        return RagSearchResponse.builder()
                .analysisId(ANALYSIS_ID)
                .totalCount(contexts.length)
                .contexts(List.of(contexts))
                .build();
    }

    private RagContextItem context(
            String ocrIngredient,
            String ingredientName,
            String description,
            Double similarityScore
    ) {
        return RagContextItem.builder()
                .ocrIngredient(ocrIngredient)
                .ingredientName(ingredientName)
                .description(description)
                .similarityScore(similarityScore)
                .build();
    }

    private PersonalizedAnalysisResult storedResult(Analysis analysis) throws Exception {
        return objectMapper.readValue(
                analysis.getAiAnalysisResult(),
                PersonalizedAnalysisResult.class
        );
    }
}
