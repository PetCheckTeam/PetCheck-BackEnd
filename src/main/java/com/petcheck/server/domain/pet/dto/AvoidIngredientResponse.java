// 7월 27일 월요일 14:37 추가
// 추가 이유: 등록 또는 조회된 기피 성분 정보를 API 응답 형식으로 반환하기 위해 추가했습니다.
// 주요 기능: 성분 ID, 표준 성분명, 설명, 기피 성분 등록 시각을 응답 데이터로 변환합니다.
package com.petcheck.server.domain.pet.dto;

import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import lombok.Getter;
//7월 27일 월요일 14:37 추가
@Getter
public class AvoidIngredientResponse {

    private final Long ingredientId;
    private final String standardName;
    private final String description;
    private final String createdAt;

    public AvoidIngredientResponse(PetAvoidIngredient petAvoidIngredient) {
        this.ingredientId = petAvoidIngredient.getIngredient().getId();
        this.standardName = petAvoidIngredient.getIngredient().getStandardName();
        this.description = petAvoidIngredient.getIngredient().getDescription();
        this.createdAt = petAvoidIngredient.getCreatedAt() != null
                ? petAvoidIngredient.getCreatedAt().toString()
                : null;
    }

    public static AvoidIngredientResponse from(PetAvoidIngredient petAvoidIngredient) {
        return new AvoidIngredientResponse(petAvoidIngredient);
    }
}
