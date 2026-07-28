// 7월 28일 화요일 수정
// 수정 내용: 반려동물 수정 요청에 선택 입력 가능한 알러지 정보를 추가했습니다.
// 수정 이유: 기존 반려동물의 알러지 정보를 수정하거나 비울 수 있도록 하기 위함입니다.
package com.petcheck.server.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetUpdateRequest {

    @NotBlank(message = "수정할 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "수정할 종(species)은 필수입니다.")
    private String species;

    @Size(max = 255, message = "알러지 정보는 255자 이하여야 합니다.")
    private String allergy;
}
