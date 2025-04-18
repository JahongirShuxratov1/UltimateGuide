package com.example.ultimateguide.mapper;

import com.example.ultimateguide.dto.AcademicInfoDto;
import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.entity.AcademicInfo;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import com.example.ultimateguide.entity.PersonalInfo;
import com.example.ultimateguide.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.ACCESSOR_ONLY)
public interface UserMapper {
    
    @Mapping(target = "academicInfo", source = "academicInfo")
    @Mapping(target = "extracurricularInfo", source = "extracurricularInfo")
    @Mapping(target = "personalInfo", source = "personalInfo")
    UserDto toDto(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "collegeRecommendations", ignore = true)
    @Mapping(target = "academicInfo", source = "academicInfo")
    @Mapping(target = "extracurricularInfo", source = "extracurricularInfo")
    @Mapping(target = "personalInfo", source = "personalInfo")
    User toEntity(UserDto userDto);
    
    AcademicInfoDto toAcademicInfoDto(AcademicInfo academicInfo);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    AcademicInfo toAcademicInfo(AcademicInfoDto academicInfoDto);
    
    @Mapping(target = "leadershipRoles", source = "leadershipRoles")
    ExtracurricularInfoDto toExtracurricularInfoDto(ExtracurricularInfo extracurricularInfo);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "leadershipRoles", source = "leadershipRoles")
    ExtracurricularInfo toExtracurricularInfo(ExtracurricularInfoDto extracurricularInfoDto);
    
    @Mapping(target = "countriesOfInterest", source = "countriesOfInterest")
    PersonalInfoDto toPersonalInfoDto(PersonalInfo personalInfo);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "countriesOfInterest", source = "countriesOfInterest")
    PersonalInfo toPersonalInfo(PersonalInfoDto personalInfoDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "collegeRecommendations", ignore = true)
    void updateUserFromDto(UserDto userDto, @MappingTarget User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateAcademicInfoFromDto(AcademicInfoDto academicInfoDto, @MappingTarget AcademicInfo academicInfo);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "leadershipRoles", source = "leadershipRoles")
    void updateExtracurricularInfoFromDto(ExtracurricularInfoDto extracurricularInfoDto, @MappingTarget ExtracurricularInfo extracurricularInfo);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "countriesOfInterest", source = "countriesOfInterest")
    void updatePersonalInfoFromDto(PersonalInfoDto personalInfoDto, @MappingTarget PersonalInfo personalInfo);

    @AfterMapping
    default void linkUser(@MappingTarget User user) {
        if (user.getAcademicInfo() != null) {
            user.getAcademicInfo().setUser(user);
        }
        if (user.getExtracurricularInfo() != null) {
            user.getExtracurricularInfo().setUser(user);
        }
        if (user.getPersonalInfo() != null) {
            user.getPersonalInfo().setUser(user);
        }
    }
} 