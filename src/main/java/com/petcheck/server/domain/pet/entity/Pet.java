package com.petcheck.server.domain.pet.entity;

import com.petcheck.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member; // 소유자 (USERS 테이블과 연동)

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species; // 예: DOG, CAT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Pet(Member member, String name, String species) {
        this.member = member;
        this.name = name;
        this.species = species;
    }

    // 수정 비즈니스 메서드
    public void updateProfile(String name, String species) {
        this.name = name;
        this.species = species;
    }
}