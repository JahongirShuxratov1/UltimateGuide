package com.example.ultimateguide.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalInfoDto {
    private Long id;
    private String major;
    private List<String> countriesOfInterest = new ArrayList<>();
    private String financialState;
} 