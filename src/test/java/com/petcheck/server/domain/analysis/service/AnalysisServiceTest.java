package com.petcheck.server.domain.analysis.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.client.RagClientException;
import com.petcheck.server.domain.analysis.dto.RagContextItem;
import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.dto.RagSearchResponse;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.repository.PetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PetRepository petRepository;
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
                imageUploadService,
                ocrService,
                ragClient,
                objectMapper
        );
    }

    @Test
    void confirm_성공_후_COMPLETED와_RAG_JSON을_저장한다() throws Exception {
        Analysis analysis = analysis("원료명: 닭고기, 쌀");
        when(analysisRepository.findByIdAndMemberId(10L, 7L)).thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenReturn(ragResponse());

        AnalysisStatus status = analysisService.confirmAndStartAi(7L, 10L);

        assertThat(status).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.getAiAnalysisResult()).isNotBlank();
        JsonNode storedJson = objectMapper.readTree(analysis.getAiAnalysisResult());
        assertThat(storedJson.get("analysisId").asLong()).isEqualTo(10L);
        assertThat(storedJson.get("contexts").get(0).get("ingredientName").asText()).isEqualTo("닭");

        ArgumentCaptor<RagSearchRequest> requestCaptor = ArgumentCaptor.forClass(RagSearchRequest.class);
        verify(ragClient).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAnalysisId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().getOcrText()).isEqualTo("원료명: 닭고기, 쌀");
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(1);
        assertThat(requestCaptor.getValue().getPetType()).isEqualTo("DOG");
    }

    @Test
    void OCR_텍스트가_비면_FAILED로_변경한다() {
        Analysis analysis = analysis("  ");
        when(analysisRepository.findByIdAndMemberId(10L, 7L)).thenReturn(Optional.of(analysis));

        AnalysisStatus status = analysisService.confirmAndStartAi(7L, 10L);

        assertThat(status).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getAiAnalysisResult()).isNull();
        verifyNoInteractions(ragClient);
    }

    @Test
    void RAG_실패_후_FAILED로_변경한다() {
        Analysis analysis = analysis("원료명: 닭고기");
        when(analysisRepository.findByIdAndMemberId(10L, 7L)).thenReturn(Optional.of(analysis));
        when(ragClient.search(any())).thenThrow(new RagClientException(
                RagClientException.Type.CONNECTION,
                "RAG 서버에 연결할 수 없습니다."
        ));

        AnalysisStatus status = analysisService.confirmAndStartAi(7L, 10L);

        assertThat(status).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getAiAnalysisResult()).isNull();
    }

    private Analysis analysis(String ocrText) {
        Pet pet = Pet.builder()
                .name("보리")
                .species("DOG")
                .build();

        return Analysis.builder()
                .pet(pet)
                .productName("샘플 사료")
                .imageUrl("https://example.com/ingredient.jpg")
                .ocrRawResult(ocrText)
                .status(AnalysisStatus.OCR_COMPLETED)
                .build();
    }

    private RagSearchResponse ragResponse() {
        return RagSearchResponse.builder()
                .analysisId(10L)
                .extractedIngredients(List.of("닭고기"))
                .totalCount(1)
                .contexts(List.of(RagContextItem.builder()
                        .ocrIngredient("닭고기")
                        .ingredientName("닭")
                        .safetyLevel("주의")
                        .description("닭 유래 원료")
                        .similarityScore(1.0)
                        .build()))
                .build();
    }
}
