package com.petcheck.server.domain.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OcrUpdateRequest {
    private String editedOcrResult; // 수정된 성분 목록
}