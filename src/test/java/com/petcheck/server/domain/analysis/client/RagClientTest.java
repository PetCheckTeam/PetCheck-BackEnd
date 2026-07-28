package com.petcheck.server.domain.analysis.client;

import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.dto.RagSearchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
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

    private RagSearchRequest request() {
        return RagSearchRequest.builder()
                .analysisId(10L)
                .ocrText("원료명: 닭고기")
                .topK(1)
                .petType("DOG")
                .build();
    }
}
