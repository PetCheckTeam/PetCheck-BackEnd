// 7월 27일 월요일 14:37 추가
// 추가 이유: 요청으로 전달받은 성분 ID의 존재 여부를 데이터베이스에서 확인하기 위해 추가했습니다.
// 주요 기능: Ingredient 엔티티의 조회 및 기본 CRUD 기능을 제공합니다.
package com.petcheck.server.domain.ingredient.repository;

import com.petcheck.server.domain.ingredient.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
