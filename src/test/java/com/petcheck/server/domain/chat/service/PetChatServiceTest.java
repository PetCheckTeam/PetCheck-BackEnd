package com.petcheck.server.domain.chat.service;

import com.petcheck.server.domain.analysis.client.RagClient;
import com.petcheck.server.domain.analysis.client.RagClientException;
import com.petcheck.server.domain.chat.dto.ChatHistoryMessage;
import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.ChatTokenUsage;
import com.petcheck.server.domain.chat.dto.RagChatResponse;
import com.petcheck.server.domain.chat.dto.RagPetChatRequest;
import com.petcheck.server.domain.ingredient.entity.Ingredient;
import com.petcheck.server.domain.member.entity.Member;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetChatServiceTest {

    private static final Long MEMBER_ID = 7L;
    private static final Long PET_ID = 5L;

    @Mock
    private PetRepository petRepository;
    @Mock
    private PetAvoidIngredientRepository petAvoidIngredientRepository;
    @Mock
    private RagClient ragClient;

    private PetChatService petChatService;

    @BeforeEach
    void setUp() {
        petChatService = new PetChatService(
                petRepository,
                petAvoidIngredientRepository,
                ragClient
        );
    }

    @Test
    void 본인_Pet_정보와_DB_회피_성분으로_일반_상담한다() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(MEMBER_ID)));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of(
                        avoidIngredient(1L, "닭고기"),
                        avoidIngredient(2L, "밀")
                ));
        when(ragClient.petChat(any())).thenReturn(ragResponse("안전한 원료를 확인해 주세요."));
        ChatRequest request = ChatRequest.builder()
                .message("오늘 저녁에는 어떤 간식을 주는 게 좋을까?")
                .history(List.of(
                        ChatHistoryMessage.builder()
                                .role("user")
                                .content("보리가 피해야 하는 성분을 알려줘")
                                .build(),
                        ChatHistoryMessage.builder()
                                .role("assistant")
                                .content("닭고기와 밀을 피해야 합니다.")
                                .build()
                ))
                .build();

        ChatResponse response = petChatService.chat(MEMBER_ID, PET_ID, request);

        assertThat(response.getAnswer()).isEqualTo("안전한 원료를 확인해 주세요.");
        assertThat(response.getSources()).isEmpty();
        ArgumentCaptor<RagPetChatRequest> captor = ArgumentCaptor.forClass(
                RagPetChatRequest.class
        );
        verify(ragClient).petChat(captor.capture());
        RagPetChatRequest ragRequest = captor.getValue();
        assertThat(ragRequest.getPetId()).isEqualTo(PET_ID);
        assertThat(ragRequest.getPetName()).isEqualTo("보리");
        assertThat(ragRequest.getPetType()).isEqualTo("DOG");
        assertThat(ragRequest.getAvoidIngredients()).containsExactly("닭고기", "밀");
        assertThat(ragRequest.getQuestion()).isEqualTo(request.getMessage());
        assertThat(ragRequest.getHistory()).containsExactlyElementsOf(request.getHistory());
    }

    @Test
    void 존재하지_않는_Pet은_404를_반환한다() {
        when(petRepository.findById(999L)).thenReturn(Optional.empty());

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, 999L, request()),
                HttpStatus.NOT_FOUND
        );
        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    @Test
    void 다른_사용자의_Pet은_403으로_차단한다() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(8L)));

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, PET_ID, request()),
                HttpStatus.FORBIDDEN
        );
        verifyNoInteractions(petAvoidIngredientRepository, ragClient);
    }

    @Test
    void 회피_성분이_없는_Pet도_빈_목록으로_정상_상담한다() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(MEMBER_ID)));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of());
        when(ragClient.petChat(any())).thenReturn(ragResponse("등록된 회피 성분이 없습니다."));

        ChatResponse response = petChatService.chat(MEMBER_ID, PET_ID, request());

        assertThat(response.getAnswer()).contains("없습니다");
        ArgumentCaptor<RagPetChatRequest> captor = ArgumentCaptor.forClass(
                RagPetChatRequest.class
        );
        verify(ragClient).petChat(captor.capture());
        assertThat(captor.getValue().getAvoidIngredients()).isEmpty();
        assertThat(captor.getValue().getHistory()).isEmpty();
    }

    @Test
    void RAG_연결_오류는_502로_변환한다() {
        stubOwnedPet();
        when(ragClient.petChat(any())).thenThrow(new RagClientException(
                RagClientException.Type.CONNECTION,
                "RAG 서버에 연결할 수 없습니다."
        ));

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, PET_ID, request()),
                HttpStatus.BAD_GATEWAY
        );
    }

    @Test
    void RAG_timeout은_504로_변환한다() {
        stubOwnedPet();
        when(ragClient.petChat(any())).thenThrow(new RagClientException(
                RagClientException.Type.TIMEOUT,
                "RAG 서버 응답 시간이 초과되었습니다."
        ));

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, PET_ID, request()),
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    @Test
    void 잘못된_RAG_응답_파싱_오류는_502로_변환한다() {
        stubOwnedPet();
        when(ragClient.petChat(any())).thenThrow(new RagClientException(
                RagClientException.Type.UNEXPECTED,
                "RAG 응답을 파싱할 수 없습니다."
        ));

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, PET_ID, request()),
                HttpStatus.BAD_GATEWAY
        );
    }

    @Test
    void RAG_answer가_공백이면_502를_반환한다() {
        stubOwnedPet();
        when(ragClient.petChat(any())).thenReturn(ragResponse("   "));

        assertStatus(
                () -> petChatService.chat(MEMBER_ID, PET_ID, request()),
                HttpStatus.BAD_GATEWAY
        );
    }

    private void stubOwnedPet() {
        when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet(MEMBER_ID)));
        when(petAvoidIngredientRepository.findAllByPetIdAndPetMemberId(PET_ID, MEMBER_ID))
                .thenReturn(List.of());
    }

    private Pet pet(Long ownerId) {
        Member member = Member.builder()
                .email("member" + ownerId + "@example.com")
                .password("encoded-password")
                .nickname("회원")
                .build();
        ReflectionTestUtils.setField(member, "id", ownerId);
        Pet pet = Pet.builder()
                .member(member)
                .name("보리")
                .species("DOG")
                .build();
        ReflectionTestUtils.setField(pet, "id", PET_ID);
        return pet;
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
                .message("어떤 간식이 좋을까?")
                .build();
    }

    private RagChatResponse ragResponse(String answer) {
        return RagChatResponse.builder()
                .answer(answer)
                .model("HCX-DASH-002")
                .finishReason("stop")
                .usage(ChatTokenUsage.empty())
                .sources(List.of())
                .build();
    }

    private void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(expectedStatus));
    }
}
