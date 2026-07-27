// 7월 27일 월요일 14:37 추가
package com.petcheck.server.domain.pet.entity;

import com.petcheck.server.domain.ingredient.entity.Ingredient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "pet_avoid_ingredients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pet_avoid_ingredients_pet_ingredient",
                columnNames = {"pet_id", "ingredient_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetAvoidIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public PetAvoidIngredient(Pet pet, Ingredient ingredient) {
        this.pet = pet;
        this.ingredient = ingredient;
    }
}
