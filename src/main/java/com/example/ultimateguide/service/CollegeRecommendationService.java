package com.example.ultimateguide.service;

import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.model.UniversityRecommendation;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollegeRecommendationService {
    private final UserService userService;

    private static final double MAX_ACCEPTANCE_RATE = 0.15; // 15% maximum realistic acceptance rate
    private static final double SAT_WEIGHT = 0.40;
    private static final double GPA_WEIGHT = 0.30;
    private static final double EC_WEIGHT = 0.20;
    private static final double ESSAY_WEIGHT = 0.10;

    public byte[] generateRecommendations(Long telegramId) throws IOException {
        UserDto userDto = userService.getUserByTelegramId(telegramId);
        if (userDto == null) {
            throw new IllegalStateException("User not found");
        }

        List<UniversityRecommendation> recommendations = calculateRecommendations(userDto);
        return createExcelFile(recommendations);
    }

    private List<UniversityRecommendation> calculateRecommendations(UserDto user) {
        List<UniversityRecommendation> recommendations = new ArrayList<>();
        
        // Calculate base score
        double baseScore = calculateBaseScore(user);
        
        // Generate recommendations for different tiers
        recommendations.addAll(generateTierRecommendations(user, baseScore, "Reach", 0.05));
        recommendations.addAll(generateTierRecommendations(user, baseScore, "Target", 0.10));
        recommendations.addAll(generateTierRecommendations(user, baseScore, "Safety", 0.15));
        
        return recommendations;
    }

    private double calculateBaseScore(UserDto user) {
        double satScore = normalizeSAT(user.getAcademicInfo().getSatScore());
        double gpaScore = normalizeGPA(user.getAcademicInfo().getGpa());
        double ecScore = calculateECScore(user.getExtracurricularInfo());
        double essayScore = calculateEssayScore(user.getPersonalInfo());

        return (SAT_WEIGHT * satScore) + 
               (GPA_WEIGHT * gpaScore) + 
               (EC_WEIGHT * ecScore) + 
               (ESSAY_WEIGHT * essayScore);
    }

    private double normalizeSAT(Integer satScore) {
        if (satScore == null) return 0.0;
        // Normalize to 0-1 range, with 1600 as max
        return Math.min(1.0, satScore / 1600.0);
    }

    private double normalizeGPA(Double gpa) {
        if (gpa == null) return 0.0;
        // Normalize to 0-1 range, with 4.0 as max
        return Math.min(1.0, gpa / 4.0);
    }

    private double calculateECScore(ExtracurricularInfoDto ecInfo) {
        if (ecInfo == null) return 0.0;
        
        int score = 0;
        // Clubs (max 3 points)
        score += Math.min(3, ecInfo.getClubs().size());
        
        // Leadership roles (max 3 points)
        score += Math.min(3, ecInfo.getLeadershipRoles().size());
        
        // Volunteer work (max 2 points)
        score += Math.min(2, ecInfo.getVolunteerWork().size());
        
        // Awards (max 2 points)
        score += Math.min(2, ecInfo.getAwards().size());
        
        // Normalize to 0-1 range
        return score / 10.0;
    }

    private double calculateEssayScore(PersonalInfoDto personalInfo) {
        if (personalInfo == null) return 0.0;
        
        int score = 0;
        // Major clarity (max 3 points)
        if (personalInfo.getMajor() != null && !personalInfo.getMajor().isEmpty()) {
            score += 3;
        }
        
        // Financial planning (max 3 points)
        if (personalInfo.getFinancialState() != null && !personalInfo.getFinancialState().isEmpty()) {
            score += 3;
        }
        
        // Country focus (max 4 points)
        if (personalInfo.getCountriesOfInterest() != null && !personalInfo.getCountriesOfInterest().isEmpty()) {
            score += Math.min(4, personalInfo.getCountriesOfInterest().size());
        }
        
        // Normalize to 0-1 range
        return score / 10.0;
    }

    private List<UniversityRecommendation> generateTierRecommendations(UserDto user, double baseScore, String tier, double acceptanceRate) {
        List<UniversityRecommendation> recommendations = new ArrayList<>();
        
        // Adjust acceptance rate based on tier
        double adjustedRate = Math.min(acceptanceRate * baseScore, MAX_ACCEPTANCE_RATE);
        
        // Generate 5-7 universities per tier
        int count = tier.equals("Target") ? 7 : 5;
        
        for (int i = 0; i < count; i++) {
            UniversityRecommendation rec = new UniversityRecommendation();
            rec.setUniversityName(getUniversityName(tier, i));
            rec.setLocation(getUniversityLocation(tier, i));
            rec.setAcceptanceRate(adjustedRate);
            rec.setAnnualTuition(calculateTuition(tier));
            rec.getScholarships().addAll(generateScholarships());
            rec.setProbability(adjustedRate);
            rec.getPrograms().addAll(generatePrograms(user.getPersonalInfo().getMajor()));
            rec.getDeadlines().addAll(generateDeadlines());
            
            recommendations.add(rec);
        }
        
        return recommendations;
    }

    private String getUniversityName(String tier, int index) {
        // Implementation would include actual university names based on tier
        return String.format("University %s %d", tier, index + 1);
    }

    private String getUniversityLocation(String tier, int index) {
        // Implementation would include actual locations
        return String.format("Location %d", index + 1);
    }

    private double calculateTuition(String tier) {
        // Base tuition calculation with tier adjustments
        double baseTuition = 45000.0;
        switch (tier) {
            case "Reach": return baseTuition * 1.2;
            case "Target": return baseTuition;
            case "Safety": return baseTuition * 0.8;
            default: return baseTuition;
        }
    }

    private List<String> generateScholarships() {
        List<String> scholarships = new ArrayList<>();
        scholarships.add("Merit-based Scholarship");
        scholarships.add("International Student Scholarship");
        scholarships.add("Uzbekistan-specific Scholarship");
        return scholarships;
    }

    private List<String> generatePrograms(String major) {
        List<String> programs = new ArrayList<>();
        if (major != null) {
            programs.add(major + " Program");
            programs.add(major + " with Research Focus");
            programs.add(major + " with Industry Partnership");
        }
        return programs;
    }

    private List<String> generateDeadlines() {
        List<String> deadlines = new ArrayList<>();
        deadlines.add("Early Decision: November 1");
        deadlines.add("Regular Decision: January 15");
        deadlines.add("Rolling Admission: Until May 1");
        return deadlines;
    }

    private byte[] createExcelFile(List<UniversityRecommendation> recommendations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("College Recommendations");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"University", "Location", "Acceptance Rate", "Annual Tuition", 
                              "Scholarships", "Probability", "Programs", "Deadlines"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Fill data rows
            int rowNum = 1;
            for (UniversityRecommendation rec : recommendations) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rec.getUniversityName());
                row.createCell(1).setCellValue(rec.getLocation());
                row.createCell(2).setCellValue(String.format("%.1f%%", rec.getAcceptanceRate() * 100.0));
                row.createCell(3).setCellValue(String.format("$%.2f", rec.getAnnualTuition()));
                row.createCell(4).setCellValue(String.join(", ", rec.getScholarships()));
                row.createCell(5).setCellValue(String.format("%.1f%%", rec.getProbability() * 100.0));
                row.createCell(6).setCellValue(String.join(", ", rec.getPrograms()));
                row.createCell(7).setCellValue(String.join(", ", rec.getDeadlines()));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
} 