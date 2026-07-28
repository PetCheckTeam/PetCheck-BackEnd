package com.petcheck.server.domain.chat.controller;

import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.service.PetChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetChatController {

    private final PetChatService petChatService;

    @PostMapping("/{petId}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable Long petId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = petChatService.chat(memberId, petId, request);
        return ResponseEntity.ok(response);
    }
}
