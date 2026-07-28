// 7월 28일 화요일 수정
// 수정 내용: 반려동물 응답에서 중복 allergy 필드와 엔티티 변환 로직을 제거했습니다.
// 수정 이유: 알러지 정보는 별도의 기피 성분 조회 응답으로 제공하기 위함입니다.
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
