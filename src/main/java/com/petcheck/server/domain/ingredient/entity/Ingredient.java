// 7월 27일 월요일 14:37 추가
// 추가 이유: 반려동물의 기피 성분으로 등록할 수 있는 표준 성분 정보를 관리하기 위해 추가했습니다.
// 주요 기능: ingredients 테이블과 매핑되어 표준 성분명과 성분 설명을 저장합니다.
package com.petcheck.server.domain.ingredient.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "standard_name", nullable = false, unique = true, length = 100)
    private String standardName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    public Ingredient(String standardName, String description) {
        this.standardName = standardName;
        this.description = description;
    }
}
