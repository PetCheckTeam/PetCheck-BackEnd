package com.petcheck.server.domain.pet.service;

import com.petcheck.server.domain.member.entity.Member;
import com.petcheck.server.domain.member.repository.MemberRepository;
import com.petcheck.server.domain.pet.dto.PetCreateRequest;
import com.petcheck.server.domain.pet.dto.PetResponse;
import com.petcheck.server.domain.pet.dto.PetUpdateRequest;
import com.petcheck.server.domain.pet.entity.Pet;
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

    // 1. 반려동물 등록
    @Transactional
    public PetResponse createPet(Long memberId, PetCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Pet pet = Pet.builder()
                .member(member)
                .name(request.getName())
                .species(request.getSpecies())
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

        pet.updateProfile(request.getName(), request.getSpecies());
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

        petRepository.delete(pet);
    }
}