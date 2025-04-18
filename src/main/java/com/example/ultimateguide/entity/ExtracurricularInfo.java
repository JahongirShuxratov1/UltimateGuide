package com.example.ultimateguide.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "extracurricular_info")
@Getter
@Setter
public class ExtracurricularInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "clubs", joinColumns = @JoinColumn(name = "extracurricular_info_id"))
    @Column(name = "club")
    private List<String> clubs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "leadership_roles", joinColumns = @JoinColumn(name = "extracurricular_info_id"))
    @Column(name = "leadership_role")
    private List<String> leadershipRoles = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "volunteer_work", joinColumns = @JoinColumn(name = "extracurricular_info_id"))
    @Column(name = "work")
    private List<String> volunteerWork = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "awards", joinColumns = @JoinColumn(name = "extracurricular_info_id"))
    @Column(name = "award")
    private List<String> awards = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
} 