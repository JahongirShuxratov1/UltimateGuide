package com.example.ultimateguide.mapper;

import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.entity.PersonalInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonalInfoMapper {
    PersonalInfoDto toDto(PersonalInfo personalInfo);
    PersonalInfo toEntity(PersonalInfoDto personalInfoDto);
} 