package com.petcheck.server.domain.chat.controller;

import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.ChatTokenUsage;
import com.petcheck.server.domain.chat.service.PetChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PetChatControllerTest {

    @Mock
    private PetChatService petChatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PetChatController(petChatService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 본인_Pet으로_일반_상담하면_200을_반환한다() throws Exception {
        ChatResponse response = ChatResponse.builder()
                .answer("등록된 회피 성분을 제외한 간식을 선택해 주세요.")
                .model("HCX-DASH-002")
                .finishReason("stop")
                .usage(ChatTokenUsage.empty())
                .sources(List.of())
                .build();
        when(petChatService.chat(eq(7L), eq(5L), any())).thenReturn(response);

        mockMvc.perform(request(5L, """
                        {
                          "message": "오늘 저녁에는 어떤 간식을 주는 게 좋을까?",
                          "history": []
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer")
                        .value("등록된 회피 성분을 제외한 간식을 선택해 주세요."))
                .andExpect(jsonPath("$.model").value("HCX-DASH-002"))
                .andExpect(jsonPath("$.finishReason").value("stop"))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources").isEmpty());
    }

    @Test
    void message가_공백이면_400으로_차단한다() throws Exception {
        mockMvc.perform(request(5L, "{\"message\":\"   \",\"history\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(petChatService);
    }

    @Test
    void message가_2000자를_초과하면_400으로_차단한다() throws Exception {
        String body = "{\"message\":\"" + "가".repeat(2001) + "\",\"history\":[]}";

        mockMvc.perform(request(5L, body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(petChatService);
    }

    @Test
    void history가_10개를_초과하면_400으로_차단한다() throws Exception {
        String history = IntStream.range(0, 11)
                .mapToObj(index -> "{\"role\":\"user\",\"content\":\"질문" + index + "\"}")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String body = "{\"message\":\"질문\",\"history\":[" + history + "]}";

        mockMvc.perform(request(5L, body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(petChatService);
    }

    @Test
    void 서비스_예외의_HTTP_상태를_그대로_반환한다() throws Exception {
        when(petChatService.chat(eq(7L), eq(404L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        when(petChatService.chat(eq(7L), eq(403L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        when(petChatService.chat(eq(7L), eq(502L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
        when(petChatService.chat(eq(7L), eq(504L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT));

        mockMvc.perform(request(404L, validBody())).andExpect(status().isNotFound());
        mockMvc.perform(request(403L, validBody())).andExpect(status().isForbidden());
        mockMvc.perform(request(502L, validBody())).andExpect(status().isBadGateway());
        mockMvc.perform(request(504L, validBody())).andExpect(status().isGatewayTimeout());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(
            Long petId,
            String body
    ) {
        UsernamePasswordAuthenticationToken authentication = authentication(7L);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return post("/api/v1/pets/{petId}/chat", petId)
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String validBody() {
        return "{\"message\":\"어떤 간식이 좋을까?\",\"history\":[]}";
    }

    private UsernamePasswordAuthenticationToken authentication(Long memberId) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of());
    }
}
