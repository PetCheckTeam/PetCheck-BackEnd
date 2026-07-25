package com.petcheck.server.domain.pet.dto;

import com.petcheck.server.domain.pet.entity.Pet;
import lombok.Getter;

@Getter
public class PetResponse {

    private final Long id;
    private final String name;
    private final String species;
    private final String createdAt;

    public PetResponse(Pet pet) {
        this.id = pet.getId();
        this.name = pet.getName();
        this.species = pet.getSpecies();
        this.createdAt = pet.getCreatedAt() != null ? pet.getCreatedAt().toString() : null;
    }

    public static PetResponse from(Pet pet) {
        return new PetResponse(pet);
    }
}