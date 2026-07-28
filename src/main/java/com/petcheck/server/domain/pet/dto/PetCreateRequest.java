package com.petcheck.server.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetCreateRequest {

    @NotBlank(message = "반려동물 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "종(species)은 필수입니다.")
    private String species;

    @Size(max = 255, message = "알러지 정보는 255자 이하여야 합니다.")
    private String allergy;
}
