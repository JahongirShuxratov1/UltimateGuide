package com.example.ultimateguide.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academic_info")
@Getter
@Setter
public class AcademicInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sat_score")
    private Integer satScore;

    @Column(name = "act_score")
    private Integer actScore;

    @Column(name = "ielts_score")
    private Double ieltsScore;

    @Column(name = "gpa")
    private Double gpa;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
} 