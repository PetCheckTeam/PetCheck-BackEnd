// 7월 28일 화요일 수정
// 수정 내용: 반려동물 응답에 저장된 알러지 정보를 포함하도록 필드와 엔티티 변환 로직을 추가했습니다.
// 수정 이유: 등록·조회·수정 API의 응답에서 반려동물 알러지 정보를 확인할 수 있도록 하기 위함입니다.
package com.petcheck.server.domain.pet.dto;

import com.petcheck.server.domain.pet.entity.Pet;
import lombok.Getter;

@Getter
public class PetResponse {

    private final Long id;
    private final String name;
    private final String species;
    private final String allergy;
    private final String createdAt;

    public PetResponse(Pet pet) {
        this.id = pet.getId();
        this.name = pet.getName();
        this.species = pet.getSpecies();
        this.allergy = pet.getAllergy();
        this.createdAt = pet.getCreatedAt() != null ? pet.getCreatedAt().toString() : null;
    }

    public static PetResponse from(Pet pet) {
        return new PetResponse(pet);
    }
}
