package com.petcheck.server.domain.chat.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.dto.IngredientMatchResult;
import com.petcheck.server.domain.analysis.dto.IngredientMatchStatus;
import com.petcheck.server.domain.analysis.dto.PersonalizedAnalysisResult;
import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.entity.Analysis;
import com.petcheck.server.domain.analysis.entity.AnalysisStatus;
import com.petcheck.server.domain.analysis.repository.AnalysisRepository;
import com.petcheck.server.domain.chat.dto.ChatHistoryMessage;
import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.ChatSource;
import com.petcheck.server.domain.chat.dto.ChatTokenUsage;
import com.petcheck.server.domain.chat.dto.RagChatRequest;
import com.petcheck.server.domain.chat.dto.RagChatResponse;
import com.petcheck.server.domain.ingredient.entity.Ingredient;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import com.petcheck.server.domain.pet.repository.PetAvoidIngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long ANALYSIS_ID = 10L;
    private static final Long PET_ID = 5L;

    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private PetAvoidIngredientRepository petAvoidIngredientRepository;
    @Mock
    private RagClient ragClient;

    private ObjectMapper objectMapper;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatService = new ChatService(
                analysisRepository,
                petAvoidIngredientRepository,
                ragClient,
                objectMapper
        );
    }

    @Test
    void 본인_분석_결과와_DB_반려동물_정보로_정상_질문한다() throws Exception {
        IngredientMatchResult ingredientResult = IngredientMatchResult.builder()
                .ocrIngredient("계육분")
                .ingredientName("닭고기")
                .matchStatus(IngredientMatchStatus.MATCHED)
                .matchedAvoidIngredientId(1L)
                .matchedAvoidIngredientName("닭고기")
                .description("닭·가금류 유래 원료")
                .similarityScore(1.0)
                .build();
        PersonalizedAnalysisResult personalizedResult = PersonalizedAnalysisResult.builder()
                .analysisId(ANALYSIS_ID)
                .totalIngredientCount(2)
                .matchedCount(1)
                .matchedIngredients(List.of(ingredientResult))
                .ingredientResults(List.of(ingredientResult))
                .build();
        Analysis analysis = completedAnalysis(objectMapper.writeValueAsString(personalizedResult));
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(avoidIngredient(1L, "닭고기")));
        when(ragClient.chat(any())).thenReturn(ragResponse());
        ChatRequest request = ChatRequest.builder()
                .message("계육분을 왜 닭고기로 분류했어?")
                .history(List.of(
                        ChatHistoryMessage.builder()
                                .role("user")
                                .content("이 사료를 먹여도 돼?")
                                .build(),
                        ChatHistoryMessage.builder()
                                .role("assistant")
                                .content("등록된 회피 성분과 일치하는 원료가 있습니다.")
                                .build()
                ))
                .build();

        ChatResponse response = chatService.chat(MEMBER_ID, ANALYSIS_ID, request);

        assertThat(response.getAnswer())
                .isEqualTo("계육분은 등록된 회피 성분인 닭고기와 일치합니다.");
        assertThat(response.getModel()).isEqualTo("HCX-DASH-002");
        assertThat(response.getSources()).hasSize(1);

        ArgumentCaptor<RagChatRequest> captor = ArgumentCaptor.forClass(RagChatRequest.class);
        verify(ragClient).chat(captor.capture());
        RagChatRequest ragRequest = captor.getValue();
        assertThat(ragRequest.getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(ragRequest.getProductName()).isEqualTo("테스트 사료");
        assertThat(ragRequest.getPetName()).isEqualTo("초코");
        assertThat(ragRequest.getPetType()).isEqualTo("DOG");
        assertThat(ragRequest.getAvoidIngredients()).containsExactly("닭고기");
        assertThat(ragRequest.getOcrText()).isEqualTo("계육분, 밀글루텐");
        assertThat(ragRequest.getIngredientResults())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getOcrIngredient()).isEqualTo("계육분");
                    assertThat(result.getIngredientName()).isEqualTo("닭고기");
                    assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
                });
        assertThat(ragRequest.getQuestion()).isEqualTo("계육분을 왜 닭고기로 분류했어?");
        assertThat(ragRequest.getHistory()).hasSize(2);
        verify(ragClient, never()).search(any(RagSearchRequest.class));
    }

    @Test
    void 다른_회원의_analysisId는_접근을_차단한다() {
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, 8L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.chat(8L, ANALYSIS_ID, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");

        verify(analysisRepository, never()).findById(ANALYSIS_ID);
        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    @Test
    void 존재하지_않는_analysisId는_질문할_수_없다() {
        when(analysisRepository.findByIdAndMemberId(999L, MEMBER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.chat(MEMBER_ID, 999L, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("분석 내역");

        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    @Test
    void COMPLETED가_아닌_분석은_질문할_수_없다() {
        Analysis analysis = analysis(AnalysisStatus.AI_ANALYZING);
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> chatService.chat(MEMBER_ID, ANALYSIS_ID, request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완료된 분석");

        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    @Test
    void aiAnalysisResult가_없으면_RAG_챗을_호출하지_않는다() {
        Analysis analysis = analysis(AnalysisStatus.COMPLETED);
        when(analysisRepository.findByIdAndMemberId(ANALYSIS_ID, MEMBER_ID))
                .thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> chatService.chat(MEMBER_ID, ANALYSIS_ID, request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("분석 결과가 없습니다");

        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    private Analysis completedAnalysis(String aiAnalysisResult) {
        Analysis analysis = analysis(AnalysisStatus.OCR_COMPLETED);
        analysis.completeAiAnalysis(aiAnalysisResult);
        return analysis;
    }

    private Analysis analysis(AnalysisStatus status) {
        Pet pet = Pet.builder()
                .name("초코")
                .species("DOG")
                .build();
        ReflectionTestUtils.setField(pet, "id", PET_ID);

        Analysis analysis = Analysis.builder()
                .pet(pet)
                .productName("테스트 사료")
                .imageUrl("https://example.com/feed.jpg")
                .ocrRawResult("계육분, 밀글루텐")
                .status(status)
                .build();
        ReflectionTestUtils.setField(analysis, "id", ANALYSIS_ID);
        return analysis;
    }

    private PetAvoidIngredient avoidIngredient(Long ingredientId, String standardName) {
        Ingredient ingredient = Ingredient.builder()
                .standardName(standardName)
                .build();
        ReflectionTestUtils.setField(ingredient, "id", ingredientId);
        return PetAvoidIngredient.builder()
                .ingredient(ingredient)
                .build();
    }

    private ChatRequest request() {
        return ChatRequest.builder()
                .message("이 사료를 먹여도 돼?")
                .build();
    }

    private RagChatResponse ragResponse() {
        return RagChatResponse.builder()
                .answer("계육분은 등록된 회피 성분인 닭고기와 일치합니다.")
                .model("HCX-DASH-002")
                .finishReason("stop")
                .usage(new ChatTokenUsage(10, 5, 15))
                .sources(List.of(ChatSource.builder()
                        .ocrIngredient("계육분")
                        .ingredientName("닭고기")
                        .matchStatus(IngredientMatchStatus.MATCHED)
                        .matchedAvoidIngredientName("닭고기")
                        .description("닭·가금류 유래 원료")
                        .similarityScore(1.0)
                        .build()))
                .build();
    }
}
