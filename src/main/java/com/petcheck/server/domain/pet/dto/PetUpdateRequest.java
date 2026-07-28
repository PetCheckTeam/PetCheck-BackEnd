// 7월 28일 화요일 수정
// 수정 내용: 반려동물 수정 요청에서 중복 allergy 필드와 길이 검증을 제거했습니다.
// 수정 이유: 알러지 정보는 별도의 기피 성분 관리 기능에서 수정하기 위함입니다.
package com.petcheck.server.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetUpdateRequest {

    @NotBlank(message = "수정할 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "수정할 종(species)은 필수입니다.")
    private String species;
}
