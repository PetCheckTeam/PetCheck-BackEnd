package com.petcheck.server.domain.analysis.client;

import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.dto.RagSearchResponse;
import com.petcheck.server.domain.chat.dto.RagChatRequest;
import com.petcheck.server.domain.chat.dto.RagChatResponse;
import com.petcheck.server.domain.chat.dto.RagPetChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RagClientTest {

    private static final String BASE_URL = "http://127.0.0.1:8100";

    @Test
    void 정상_응답을_DTO로_변환한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "analysisId": 10,
                          "extractedIngredients": ["닭고기"],
                          "totalCount": 1,
                          "contexts": [
                            {
                              "ocrIngredient": "닭고기",
                              "ingredientName": "닭",
                              "safetyLevel": "주의",
                              "description": "닭 유래 원료",
                              "similarityScore": 1.0
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RagSearchResponse response = ragClient.search(request());

        assertThat(response.getAnalysisId()).isEqualTo(10L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getContexts()).singleElement().satisfies(context -> {
            assertThat(context.getIngredientName()).isEqualTo("닭");
            assertThat(context.getSafetyLevel()).isEqualTo("주의");
        });
        server.verify();
    }

    @Test
    void RAG_422_응답을_클라이언트_오류로_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/search"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"원료명을 찾지 못했습니다.\"}"));

        assertThatThrownBy(() -> ragClient.search(request()))
                .isInstanceOfSatisfying(RagClientException.class, error -> {
                    assertThat(error.getType()).isEqualTo(RagClientException.Type.CLIENT_ERROR);
                    assertThat(error.getStatusCode()).isEqualTo(422);
                    assertThat(error.getMessage())
                            .contains("HTTP 422")
                            .contains("원료명을 찾지 못했습니다.");
                });
        server.verify();
    }

    @Test
    void RAG_서버_연결_실패를_구분한다() {
        ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
            throw new ConnectException("Connection refused");
        };
        RagClient ragClient = new RagClient(new RestTemplate(requestFactory), BASE_URL);

        assertThatThrownBy(() -> ragClient.search(request()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.CONNECTION));
    }

    @Test
    void RAG_서버_타임아웃을_구분한다() {
        ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
            throw new SocketTimeoutException("Read timed out");
        };
        RagClient ragClient = new RagClient(new RestTemplate(requestFactory), BASE_URL);

        assertThatThrownBy(() -> ragClient.search(request()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.TIMEOUT));
    }

    @Test
    void RAG_500_응답을_서버_오류로_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/search"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> ragClient.search(request()))
                .isInstanceOfSatisfying(RagClientException.class, error -> {
                    assertThat(error.getType()).isEqualTo(RagClientException.Type.SERVER_ERROR);
                    assertThat(error.getStatusCode()).isEqualTo(500);
                });
        server.verify();
    }

    @Test
    void RAG_빈_응답을_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/search"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> ragClient.search(request()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.EMPTY_RESPONSE));
        server.verify();
    }

    @Test
    void RAG_챗_정상_응답을_DTO로_변환한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "answer": "등록된 회피 성분인 닭고기와 일치합니다.",
                          "model": "HCX-DASH-002",
                          "finishReason": "stop",
                          "usage": {
                            "promptTokens": 10,
                            "completionTokens": 5,
                            "totalTokens": 15
                          },
                          "sources": []
                        }
                        """, MediaType.APPLICATION_JSON));

        RagChatResponse response = ragClient.chat(chatRequest());

        assertThat(response.getAnswer()).contains("닭고기");
        assertThat(response.getModel()).isEqualTo("HCX-DASH-002");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(15);
        server.verify();
    }

    @Test
    void RAG_챗_4xx를_클라이언트_오류로_구분하고_응답_본문을_노출하지_않는다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/chat"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"internal-sensitive-detail\"}"));

        assertThatThrownBy(() -> ragClient.chat(chatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error -> {
                    assertThat(error.getType()).isEqualTo(RagClientException.Type.CLIENT_ERROR);
                    assertThat(error.getStatusCode()).isEqualTo(422);
                    assertThat(error.getMessage())
                            .contains("HTTP 422")
                            .doesNotContain("internal-sensitive-detail");
                });
        server.verify();
    }

    @Test
    void RAG_챗_5xx를_서버_오류로_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/chat"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> ragClient.chat(chatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error -> {
                    assertThat(error.getType()).isEqualTo(RagClientException.Type.SERVER_ERROR);
                    assertThat(error.getStatusCode()).isEqualTo(500);
                });
        server.verify();
    }

    @Test
    void RAG_챗_timeout을_구분한다() {
        ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
            throw new SocketTimeoutException("Read timed out");
        };
        RagClient ragClient = new RagClient(new RestTemplate(requestFactory), BASE_URL);

        assertThatThrownBy(() -> ragClient.chat(chatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.TIMEOUT));
    }

    @Test
    void RAG_챗_연결_실패를_구분한다() {
        ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
            throw new ConnectException("Connection refused");
        };
        RagClient ragClient = new RagClient(new RestTemplate(requestFactory), BASE_URL);

        assertThatThrownBy(() -> ragClient.chat(chatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.CONNECTION));
    }

    @Test
    void RAG_챗_빈_응답을_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/chat"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> ragClient.chat(chatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.EMPTY_RESPONSE));
        server.verify();
    }

    @Test
    void Pet_챗_요청을_전용_경로로_전송하고_응답을_변환한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/pet-chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "petId": 5,
                          "petName": "보리",
                          "petType": "DOG",
                          "avoidIngredients": ["닭고기", "밀"],
                          "question": "어떤 간식이 좋을까?",
                          "history": []
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "answer": "회피 성분을 제외한 간식을 선택해 주세요.",
                          "model": "HCX-DASH-002",
                          "finishReason": "stop",
                          "usage": {
                            "promptTokens": 0,
                            "completionTokens": 0,
                            "totalTokens": 0
                          },
                          "sources": []
                        }
                        """, MediaType.APPLICATION_JSON));

        RagChatResponse response = ragClient.petChat(petChatRequest());

        assertThat(response.getAnswer()).contains("회피 성분");
        assertThat(response.getSources()).isEmpty();
        server.verify();
    }

    @Test
    void Pet_챗의_잘못된_JSON_응답을_파싱_오류로_구분한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RagClient ragClient = new RagClient(restTemplate, BASE_URL);

        server.expect(requestTo(BASE_URL + "/api/v1/rag/pet-chat"))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> ragClient.petChat(petChatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.UNEXPECTED));
        server.verify();
    }

    @Test
    void Pet_챗_timeout을_구분한다() {
        ClientHttpRequestFactory requestFactory = (uri, httpMethod) -> {
            throw new SocketTimeoutException("Read timed out");
        };
        RagClient ragClient = new RagClient(new RestTemplate(requestFactory), BASE_URL);

        assertThatThrownBy(() -> ragClient.petChat(petChatRequest()))
                .isInstanceOfSatisfying(RagClientException.class, error ->
                        assertThat(error.getType()).isEqualTo(RagClientException.Type.TIMEOUT));
    }

    private RagSearchRequest request() {
        return RagSearchRequest.builder()
                .analysisId(10L)
                .ocrText("원료명: 닭고기")
                .topK(1)
                .petType("DOG")
                .build();
    }

    private RagChatRequest chatRequest() {
        return RagChatRequest.builder()
                .analysisId(10L)
                .productName("테스트 사료")
                .petName("초코")
                .petType("DOG")
                .avoidIngredients(List.of("닭고기"))
                .ocrText("계육분")
                .ingredientResults(List.of())
                .question("이 사료를 먹여도 돼?")
                .history(List.of())
                .build();
    }

    private RagPetChatRequest petChatRequest() {
        return RagPetChatRequest.builder()
                .petId(5L)
                .petName("보리")
                .petType("DOG")
                .avoidIngredients(List.of("닭고기", "밀"))
                .question("어떤 간식이 좋을까?")
                .history(List.of())
                .build();
    }
}
