package com.petcheck.server.domain.analysis.client;

import com.petcheck.server.domain.analysis.dto.RagSearchRequest;
import com.petcheck.server.domain.analysis.dto.RagSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
public class RagClient {

    private static final String SEARCH_PATH = "/api/v1/rag/search";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_ERROR_RESPONSE_LENGTH = 2000;

    private final RestTemplate restTemplate;
    private final String searchUrl;

    @Autowired
    public RagClient(@Value("${rag.base-url}") String baseUrl) {
        this(createRestTemplate(), baseUrl);
    }

    RagClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.searchUrl = normalizeBaseUrl(baseUrl) + SEARCH_PATH;
    }

    public RagSearchResponse search(RagSearchRequest requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RagSearchRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<RagSearchResponse> response = restTemplate.postForEntity(
                    searchUrl,
                    request,
                    RagSearchResponse.class
            );

            if (response.getBody() == null) {
                throw new RagClientException(
                        RagClientException.Type.EMPTY_RESPONSE,
                        "RAG 서버가 빈 응답을 반환했습니다."
                );
            }
            return response.getBody();
        } catch (HttpClientErrorException error) {
            String responseBody = summarizeErrorResponse(error.getResponseBodyAsString());
            throw new RagClientException(
                    RagClientException.Type.CLIENT_ERROR,
                    error.getStatusCode().value(),
                    "RAG 서버가 요청을 거부했습니다. HTTP "
                            + error.getStatusCode().value()
                            + ", 응답: "
                            + responseBody,
                    error
            );
        } catch (HttpServerErrorException error) {
            throw new RagClientException(
                    RagClientException.Type.SERVER_ERROR,
                    error.getStatusCode().value(),
                    "RAG 서버 내부 오류가 발생했습니다. HTTP " + error.getStatusCode().value(),
                    error
            );
        } catch (ResourceAccessException error) {
            if (isTimeout(error)) {
                throw new RagClientException(
                        RagClientException.Type.TIMEOUT,
                        "RAG 서버 응답 시간이 초과되었습니다.",
                        error
                );
            }
            throw new RagClientException(
                    RagClientException.Type.CONNECTION,
                    "RAG 서버에 연결할 수 없습니다.",
                    error
            );
        } catch (RagClientException error) {
            throw error;
        } catch (RestClientException error) {
            throw new RagClientException(
                    RagClientException.Type.UNEXPECTED,
                    "RAG 서버 호출 중 예상하지 못한 오류가 발생했습니다.",
                    error
            );
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("rag.base-url은 비어 있을 수 없습니다.");
        }
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private static String summarizeErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "(응답 본문 없음)";
        }

        String trimmedBody = responseBody.strip();
        if (trimmedBody.length() <= MAX_ERROR_RESPONSE_LENGTH) {
            return trimmedBody;
        }
        return trimmedBody.substring(0, MAX_ERROR_RESPONSE_LENGTH - 3) + "...";
    }

    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
