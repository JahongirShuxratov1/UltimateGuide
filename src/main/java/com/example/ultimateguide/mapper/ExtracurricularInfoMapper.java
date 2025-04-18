package com.example.ultimateguide.mapper;

import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExtracurricularInfoMapper {
    ExtracurricularInfoDto toDto(ExtracurricularInfo extracurricularInfo);
    ExtracurricularInfo toEntity(ExtracurricularInfoDto extracurricularInfoDto);
} 