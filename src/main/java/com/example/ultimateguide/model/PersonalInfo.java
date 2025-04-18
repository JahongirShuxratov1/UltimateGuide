package com.example.ultimateguide.model;

import com.example.ultimateguide.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class PersonalInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String major;
    private String highSchoolName;
    private String highSchoolLocation;
    private String highSchoolGrading;
    private Double highSchoolGpa;
    private String studyLanguage;
    
    @ElementCollection
    private List<String> studyFields = new ArrayList<>();
    
    private String studyDuration;
    private Integer studyStartYear;
    private String studyMode;
    private String studyLocation;
    private Double studyBudget;
    private Boolean needsScholarship;
    private String financialState;
    
    @ElementCollection
    private List<String> workExperience = new ArrayList<>();
    
    @ElementCollection
    private List<String> researchExperience = new ArrayList<>();
    
    @ElementCollection
    private List<String> internshipExperience = new ArrayList<>();
    
    @ElementCollection
    private List<String> leadershipExperience = new ArrayList<>();
    
    @ElementCollection
    private List<String> communityService = new ArrayList<>();
    
    @ElementCollection
    private List<String> awards = new ArrayList<>();
    
    @ElementCollection
    private List<String> languages = new ArrayList<>();
    
    @ElementCollection
    private List<String> standardizedTests = new ArrayList<>();
    
    @ElementCollection
    private List<String> collegeEssays = new ArrayList<>();
    
    @ElementCollection
    private List<String> recommendations = new ArrayList<>();
    
    @ElementCollection
    private List<String> portfolio = new ArrayList<>();
    
    @ElementCollection
    private List<String> countriesOfInterest = new ArrayList<>();
    
    private Boolean hasInterview;
    private Boolean hasAudition;
    private Boolean hasTrial;
    private String otherInfo;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
} 