// 7월 27일 월요일 14:37 추가
// 추가 이유: 반려동물별 기피 성분 관계 데이터를 조회하고 삭제하기 위해 추가했습니다.
// 주요 기능: 중복 등록 확인, 목록 조회, 단건 조회, 반려동물별 관계 일괄 삭제 기능을 제공합니다.
package com.petcheck.server.domain.pet.repository;

import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetAvoidIngredientRepository extends JpaRepository<PetAvoidIngredient, Long> {

    boolean existsByPetIdAndIngredientId(Long petId, Long ingredientId);

    List<PetAvoidIngredient> findAllByPetId(Long petId);

    @EntityGraph(attributePaths = "ingredient")
    List<PetAvoidIngredient> findAllByPetIdAndPetMemberId(Long petId, Long memberId);

    Optional<PetAvoidIngredient> findByPetIdAndIngredientId(Long petId, Long ingredientId);

    void deleteAllByPetId(Long petId);
}
