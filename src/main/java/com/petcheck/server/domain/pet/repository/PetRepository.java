package com.petcheck.server.domain.pet.repository;

import com.petcheck.server.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    // 특정 회원의 반려동물 목록 조회
    List<Pet> findAllByMemberId(Long memberId);

    Optional<Pet> findByIdAndMemberId(Long petId, Long memberId);
}
