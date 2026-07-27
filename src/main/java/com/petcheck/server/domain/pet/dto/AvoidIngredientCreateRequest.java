// 7월 27일 월요일 14:37 추가
package com.petcheck.server.domain.pet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AvoidIngredientCreateRequest {

    @NotNull(message = "성분 ID는 필수입니다.")
    private Long ingredientId;
}
