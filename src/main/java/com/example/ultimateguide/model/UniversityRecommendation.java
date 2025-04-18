package com.example.ultimateguide.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class UniversityRecommendation {
    private String universityName;
    private String location;
    private double acceptanceRate;
    private double annualTuition;
    private List<String> scholarships = new ArrayList<>();
    private double probability;
    private List<String> programs = new ArrayList<>();
    private List<String> deadlines = new ArrayList<>();
} 