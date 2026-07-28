// 7월 27일 월요일 14:37 수정
// 수정 내용: 반려동물 삭제 전에 해당 반려동물의 기피 성분 관계를 먼저 삭제하도록 처리했습니다.
// 수정 이유: pet_avoid_ingredients의 pet_id 외래키로 인한 반려동물 삭제 오류를 방지하기 위함입니다.
// 7월 28일 화요일 수정
// 수정 내용: 반려동물 등록·수정 요청의 알러지 정보를 Pet 엔티티에 반영하도록 처리했습니다.
// 수정 이유: 요청 DTO부터 pets 테이블까지 알러지 정보가 정상적으로 저장·수정되도록 하기 위함입니다.
package com.petcheck.server.domain.pet.service;

import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.dto.PetCreateRequest;
import com.petcheck.server.domain.pet.dto.PetResponse;
import com.petcheck.server.domain.pet.dto.PetUpdateRequest;
import com.petcheck.server.domain.pet.entity.Pet;
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
public class PetService {

    private final PetRepository petRepository;
    private final MemberRepository memberRepository;
    private final PetAvoidIngredientRepository petAvoidIngredientRepository;

    // 1. 반려동물 등록
    @Transactional
    public PetResponse createPet(Long memberId, PetCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Pet pet = Pet.builder()
                .member(member)
                .name(request.getName())
                .species(request.getSpecies())
                .allergy(request.getAllergy())
                .build();

        Pet savedPet = petRepository.save(pet);
        return PetResponse.from(savedPet);
    }

    // 2. 반려동물 목록 조회
    public List<PetResponse> getPets(Long memberId) {
        List<Pet> pets = petRepository.findAllByMemberId(memberId);
        return pets.stream()
                .map(PetResponse::from)
                .collect(Collectors.toList());
    }

    // 3. 반려동물 상세 조회
    public PetResponse getPetDetail(Long memberId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));

        // 본인 소유의 반려동물이 맞는지 검증
        if (!pet.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("접근 권한이 없는 반려동물입니다.");
        }

        return PetResponse.from(pet);
    }

    // 4. 반려동물 수정
    @Transactional
    public PetResponse updatePet(Long memberId, Long petId, PetUpdateRequest request) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));

        if (!pet.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("수정 권한이 없는 반려동물입니다.");
        }

        pet.updateProfile(request.getName(), request.getSpecies(), request.getAllergy());
        return PetResponse.from(pet);
    }

    // 5. 반려동물 삭제
    @Transactional
    public void deletePet(Long memberId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다."));

        if (!pet.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("삭제 권한이 없는 반려동물입니다.");
        }

        petAvoidIngredientRepository.deleteAllByPetId(petId);
        petRepository.delete(pet);
    }
}
