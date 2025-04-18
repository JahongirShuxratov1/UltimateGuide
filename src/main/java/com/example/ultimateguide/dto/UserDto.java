package com.example.ultimateguide.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private Long telegramId;
    private String username;
    private String phoneNumber;
    private AcademicInfoDto academicInfo;
    private ExtracurricularInfoDto extracurricularInfo;
    private PersonalInfoDto personalInfo;
} 