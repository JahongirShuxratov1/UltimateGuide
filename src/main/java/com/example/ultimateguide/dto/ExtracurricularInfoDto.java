package com.example.ultimateguide.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExtracurricularInfoDto {
    private Long id;
    private List<String> clubs = new ArrayList<>();
    private List<String> leadershipRoles = new ArrayList<>();
    private List<String> volunteerWork = new ArrayList<>();
    private List<String> awards = new ArrayList<>();
} 