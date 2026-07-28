package com.petcheck.server.domain.chat.controller;

import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.dto.ChatTokenUsage;
import com.petcheck.server.domain.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ChatController(chatService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 본인_분석에_질문하면_200과_챗봇_응답을_반환한다() throws Exception {
        ChatResponse response = ChatResponse.builder()
                .answer("등록된 회피 성분인 닭고기와 일치합니다.")
                .model("HCX-DASH-002")
                .finishReason("stop")
                .usage(ChatTokenUsage.empty())
                .sources(List.of())
                .build();
        when(chatService.chat(eq(7L), eq(10L), any())).thenReturn(response);
        SecurityContextHolder.getContext().setAuthentication(authentication(7L));

        mockMvc.perform(post("/api/v1/analyses/10/chat")
                        .principal(authentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "이 사료를 먹여도 돼?",
                                  "history": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer")
                        .value("등록된 회피 성분인 닭고기와 일치합니다."))
                .andExpect(jsonPath("$.model").value("HCX-DASH-002"))
                .andExpect(jsonPath("$.finishReason").value("stop"));
    }

    @Test
    void message가_공백이면_요청을_거부한다() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/10/chat")
                        .principal(authentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \",\"history\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatService);
    }

    @Test
    void history_role이_user나_assistant가_아니면_요청을_거부한다() throws Exception {
        mockMvc.perform(post("/api/v1/analyses/10/chat")
                        .principal(authentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "이 사료를 먹여도 돼?",
                                  "history": [
                                    {"role": "system", "content": "잘못된 역할"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatService);
    }

    private UsernamePasswordAuthenticationToken authentication(Long memberId) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of());
    }
}
