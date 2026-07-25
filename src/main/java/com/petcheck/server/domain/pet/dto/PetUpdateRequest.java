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

    public PetUpdateRequest(String name, String species) {
        this.name = name;
        this.species = species;
    }
}