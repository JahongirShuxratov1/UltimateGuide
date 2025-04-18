package com.example.ultimateguide.mapper;

import com.example.ultimateguide.dto.AcademicInfoDto;
import com.example.ultimateguide.entity.AcademicInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcademicInfoMapper {
    AcademicInfoDto toDto(AcademicInfo academicInfo);
    AcademicInfo toEntity(AcademicInfoDto academicInfoDto);
} 