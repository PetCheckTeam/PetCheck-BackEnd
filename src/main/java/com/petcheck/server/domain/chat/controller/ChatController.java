package com.petcheck.server.domain.chat.controller;

import com.petcheck.server.domain.chat.dto.ChatRequest;
import com.petcheck.server.domain.chat.dto.ChatResponse;
import com.petcheck.server.domain.chat.service.ChatService;
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
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{analysisId}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable Long analysisId,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ChatRequest request
    ) {
        ChatResponse response = chatService.chat(memberId, analysisId, request);
        return ResponseEntity.ok(response);
    }
}
