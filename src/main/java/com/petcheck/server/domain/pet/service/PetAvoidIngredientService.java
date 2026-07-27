// 7월 27일 월요일 14:37 추가
// 추가 이유: 반려동물별 기피 성분 등록, 조회, 삭제의 비즈니스 로직을 분리하기 위해 추가했습니다.
// 주요 기능: 반려동물 존재 및 소유권, 성분 존재, 중복 등록 여부를 검증하고 기피 성분 관계를 관리합니다.
package com.petcheck.server.domain.pet.service;

import com.petcheck.server.domain.ingredient.entity.Ingredient;
import com.petcheck.server.domain.ingredient.repository.IngredientRepository;
import com.petcheck.server.domain.pet.dto.AvoidIngredientCreateRequest;
import com.petcheck.server.domain.pet.dto.AvoidIngredientResponse;
import com.petcheck.server.domain.pet.entity.Pet;
import com.petcheck.server.domain.pet.entity.PetAvoidIngredient;
import com.petcheck.server.domain.pet.repository.PetAvoidIngredientRepository;
import com.petcheck.server.domain.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetAvoidIngredientService {

    private final PetRepository petRepository;
    private final IngredientRepository ingredientRepository;
    private final PetAvoidIngredientRepository petAvoidIngredientRepository;

    // 1. 기피 성분 등록
    @Transactional
    public AvoidIngredientResponse createAvoidIngredient(
            Long memberId,
            Long petId,
            AvoidIngredientCreateRequest request) {
        Pet pet = findPetWithAuth(memberId, petId);

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 성분입니다."));

        if (petAvoidIngredientRepository.existsByPetIdAndIngredientId(petId, ingredient.getId())) {
            throw new IllegalArgumentException("이미 등록된 기피 성분입니다.");
        }

        PetAvoidIngredient petAvoidIngredient = PetAvoidIngredient.builder()
                .pet(pet)
                .ingredient(ingredient)
                .build();

        PetAvoidIngredient savedPetAvoidIngredient = petAvoidIngredientRepository.save(petAvoidIngredient);
        return AvoidIngredientResponse.from(savedPetAvoidIngredient);
    }

    // 2. 기피 성분 목록 조회
    public List<AvoidIngredientResponse> getAvoidIngredients(Long memberId, Long petId) {
        findPetWithAuth(memberId, petId);

        List<PetAvoidIngredient> petAvoidIngredients = petAvoidIngredientRepository.findAllByPetId(petId);
        return petAvoidIngredients.stream()
                .map(AvoidIngredientResponse::from)
                .collect(Collectors.toList());
    }

    // 3. 기피 성분 삭제
    @Transactional
    public void deleteAvoidIngredient(Long memberId, Long petId, Long ingredientId) {
        findPetWithAuth(memberId, petId);

        PetAvoidIngredient petAvoidIngredient = petAvoidIngredientRepository
                .findByPetIdAndIngredientId(petId, ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기피 성분입니다."));

        petAvoidIngredientRepository.delete(petAvoidIngredient);
    }

    private Pet findPetWithAuth(Long memberId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));

        if (!pet.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("접근 권한이 없는 반려동물입니다.");
        }

        return pet;
    }
}
