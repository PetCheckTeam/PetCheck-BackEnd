package com.petcheck.server.domain.analysis.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NcpClovaOcrServiceImplTest {

    private final NcpClovaOcrServiceImpl ocrService = new NcpClovaOcrServiceImpl();

    @Test
    void inferText를_순서대로_추출하고_구분자를_유지한다() {
        String responseBody = """
                {
                  "images": [
                    {
                      "fields": [
                        {"inferText": "원료명", "lineBreak": true},
                        {"inferText": "닭고기"},
                        {"inferText": ","},
                        {"inferText": "쌀", "lineBreak": true},
                        {"inferText": "비타민 E"}
                      ]
                    }
                  ]
                }
                """;

        String result = ocrService.parseOcrResponse(responseBody);

        assertThat(result).isEqualTo("원료명\n닭고기, 쌀\n비타민 E");
    }

    @Test
    void OCR_응답이_비어_있으면_예외가_발생한다() {
        assertThatThrownBy(() -> ocrService.parseOcrResponse(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("응답이 비어");
    }

    @Test
    void inferText가_없으면_예외가_발생한다() {
        String responseBody = """
                {"images":[{"fields":[{"inferText":"  "}]}]}
                """;

        assertThatThrownBy(() -> ocrService.parseOcrResponse(responseBody))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inferText");
    }
}
