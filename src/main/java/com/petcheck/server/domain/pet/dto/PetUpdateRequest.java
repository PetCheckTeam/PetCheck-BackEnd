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
