package com.petcheck.server.domain.analysis.service;

public interface OcrService {
    String extractTextFromImage(String imageUrl);
}