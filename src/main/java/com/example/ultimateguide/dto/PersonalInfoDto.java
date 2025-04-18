package com.example.ultimateguide.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalInfoDto {
    private Long id;
    private String major;
    private String highSchoolName;
    private String highSchoolLocation;
    private String highSchoolGrading;
    private Double highSchoolGpa;
    private String studyLanguage;
    private List<String> studyFields = new ArrayList<>();
    private String studyDuration;
    private Integer studyStartYear;
    private String studyMode;
    private String studyLocation;
    private Double studyBudget;
    private Boolean needsScholarship;
    private String financialState;
    private List<String> workExperience = new ArrayList<>();
    private List<String> researchExperience = new ArrayList<>();
    private List<String> internshipExperience = new ArrayList<>();
    private List<String> leadershipExperience = new ArrayList<>();
    private List<String> communityService = new ArrayList<>();
    private List<String> awards = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private List<String> standardizedTests = new ArrayList<>();
    private List<String> collegeEssays = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private List<String> portfolio = new ArrayList<>();
    private List<String> countriesOfInterest = new ArrayList<>();
    private Boolean hasInterview;
    private Boolean hasAudition;
    private Boolean hasTrial;
    private String otherInfo;
} 