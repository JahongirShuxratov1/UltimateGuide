package com.example.ultimateguide.service;

import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.dto.AcademicInfoDto;
import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.entity.User;
import com.example.ultimateguide.entity.AcademicInfo;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import com.example.ultimateguide.entity.PersonalInfo;
import com.example.ultimateguide.mapper.UserMapper;
import com.example.ultimateguide.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDto getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(userMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public UserDto createUser(User user) {
        if (user.getPhoneNumber() != null && isPhoneNumberUsed(user.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        if (isUserRegistered(user.getTelegramId())) {
            throw new IllegalArgumentException("User is already registered");
        }

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long telegramId, UserDto userDto) {
        User user = findUserByTelegramId(telegramId);
        userMapper.updateUserFromDto(userDto, user);

        if (userDto.getAcademicInfo() != null) {
            if (user.getAcademicInfo() == null) {
                AcademicInfo academicInfo = userMapper.toAcademicInfo(userDto.getAcademicInfo());
                academicInfo.setUser(user);
                user.setAcademicInfo(academicInfo);
            } else {
                userMapper.updateAcademicInfoFromDto(userDto.getAcademicInfo(), user.getAcademicInfo());
            }
        }

        if (userDto.getExtracurricularInfo() != null) {
            if (user.getExtracurricularInfo() == null) {
                ExtracurricularInfo extracurricularInfo = userMapper.toExtracurricularInfo(userDto.getExtracurricularInfo());
                extracurricularInfo.setUser(user);
                user.setExtracurricularInfo(extracurricularInfo);
            } else {
                userMapper.updateExtracurricularInfoFromDto(userDto.getExtracurricularInfo(), user.getExtracurricularInfo());
            }
        }

        if (userDto.getPersonalInfo() != null) {
            if (user.getPersonalInfo() == null) {
                PersonalInfo personalInfo = userMapper.toPersonalInfo(userDto.getPersonalInfo());
                personalInfo.setUser(user);
                user.setPersonalInfo(personalInfo);
            } else {
                userMapper.updatePersonalInfoFromDto(userDto.getPersonalInfo(), user.getPersonalInfo());
            }
        }

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public User findUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with telegram ID: " + telegramId));
    }

    @Transactional(readOnly = true)
    public boolean isUserRegistered(Long telegramId) {
        return userRepository.existsByTelegramId(telegramId);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneNumberUsed(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Transactional
    public void updateFinancialAidStatus(Long telegramId, String financialState) {
        User user = findUserByTelegramId(telegramId);
        
        if (user.getPersonalInfo() == null) {
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setUser(user);
            user.setPersonalInfo(personalInfo);
        }
        
        user.getPersonalInfo().setFinancialState(financialState);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
} 