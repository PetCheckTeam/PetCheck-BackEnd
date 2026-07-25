package com.petcheck.server.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetCreateRequest {

    @NotBlank(message = "반려동물 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "종(species)은 필수입니다.")
    private String species;

    public PetCreateRequest(String name, String species) {
        this.name = name;
        this.species = species;
    }
}