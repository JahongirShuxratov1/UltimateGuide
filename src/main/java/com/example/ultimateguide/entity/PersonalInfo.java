package com.example.ultimateguide.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personal_info")
@Getter
@Setter
public class PersonalInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String major;

    @ElementCollection
    @CollectionTable(name = "countries_of_interest", joinColumns = @JoinColumn(name = "personal_info_id"))
    @Column(name = "country")
    private List<String> countriesOfInterest = new ArrayList<>();

    private String financialState;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
} 