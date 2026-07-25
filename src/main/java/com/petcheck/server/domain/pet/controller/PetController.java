package com.petcheck.server.domain.pet.controller;

import com.petcheck.server.domain.pet.dto.PetCreateRequest;
import com.petcheck.server.domain.pet.dto.PetResponse;
import com.petcheck.server.domain.pet.dto.PetUpdateRequest;
import com.petcheck.server.domain.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    // 1. 반려동물 등록
    @PostMapping
    public ResponseEntity<PetResponse> createPet(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PetCreateRequest request) {
        PetResponse response = petService.createPet(memberId, request);
        return ResponseEntity.ok(response);
    }

    // 2. 반려동물 목록 조회
    @GetMapping
    public ResponseEntity<List<PetResponse>> getPets(
            @AuthenticationPrincipal Long memberId) {
        List<PetResponse> response = petService.getPets(memberId);
        return ResponseEntity.ok(response);
    }

    // 3. 반려동물 상세 조회
    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPetDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long petId) {
        PetResponse response = petService.getPetDetail(memberId, petId);
        return ResponseEntity.ok(response);
    }

    // 4. 반려동물 수정
    @PatchMapping("/{petId}")
    public ResponseEntity<PetResponse> updatePet(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request) {
        PetResponse response = petService.updatePet(memberId, petId, request);
        return ResponseEntity.ok(response);
    }

    // 5. 반려동물 삭제
    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long petId) {
        petService.deletePet(memberId, petId);
        return ResponseEntity.noContent().build();
    }
}