package com.example.ultimateguide.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "college_recommendations")
public class CollegeRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private CollegeCategory category;

    @Column(name = "match_percentage")
    private Double matchPercentage;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public enum CollegeCategory {
        REACH,
        MATCH,
        SAFETY
    }
} 