// 7월 28일 화요일 수정
// 수정 내용: pets 테이블의 중복 allergy 필드와 생성·수정 로직을 제거했습니다.
// 수정 이유: 반려동물의 알러지 정보는 별도의 기피 성분 테이블에서 관리하기 위함입니다.
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
