package com.example.ultimateguide.service;

import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import com.example.ultimateguide.entity.User;
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

    public byte[] generateRecommendations(Long telegramId) throws IOException {
        UserDto userDto = userService.getUserByTelegramId(telegramId);
        if (userDto == null) {
            throw new IllegalStateException("User not found");
        }

        // Create workbook and sheet
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("College Recommendations");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"University Name", "Location", "Acceptance Rate", "Annual Tuition", 
                              "Available Scholarships", "Admission Probability", "Recommended Programs", "Application Deadlines"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Get recommendations based on user profile
            List<CollegeRecommendation> recommendations = getRecommendationsForUser(userDto);

            // Fill data rows
            int rowNum = 1;
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);

            for (CollegeRecommendation rec : recommendations) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rec.getUniversityName());
                row.createCell(1).setCellValue(rec.getLocation());
                row.createCell(2).setCellValue(rec.getAcceptanceRate() + "%");
                row.createCell(3).setCellValue("$" + rec.getAnnualTuition());
                row.createCell(4).setCellValue(rec.getScholarships());
                row.createCell(5).setCellValue(rec.getProbability() + "%");
                row.createCell(6).setCellValue(rec.getPrograms());
                row.createCell(7).setCellValue(rec.getDeadlines());

                // Apply style to all cells in the row
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Write to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<CollegeRecommendation> getRecommendationsForUser(UserDto user) {
        List<CollegeRecommendation> recommendations = new ArrayList<>();
        
        // Get user's academic profile
        Double gpa = user.getAcademicInfo().getGpa();
        Integer satScore = user.getAcademicInfo().getSatScore();
        Integer actScore = user.getAcademicInfo().getActScore();
        Double ieltsScore = user.getAcademicInfo().getIeltsScore();
        
        // Get user's interests and preferences
        String major = user.getPersonalInfo().getMajor();
        List<String> countries = user.getPersonalInfo().getCountriesOfInterest();
        String financialState = user.getPersonalInfo().getFinancialState();
        
        // Check extracurricular strength
        boolean hasStrongExtracurriculars = hasStrongExtracurriculars(user.getExtracurricularInfo());

        // Top tier universities (highly selective)
        if ((gpa != null && gpa >= 3.8) && 
            ((satScore != null && satScore >= 1500) || (actScore != null && actScore >= 34))) {
            
            recommendations.add(new CollegeRecommendation(
                "Harvard University",
                "Cambridge, MA, USA",
                5,
                55000,
                "Need-based aid available up to full tuition",
                hasStrongExtracurriculars ? 75 : 60,
                "Computer Science, Economics, Mathematics, Liberal Arts",
                "Regular Decision: January 1, Early Action: November 1"
            ));
            
            recommendations.add(new CollegeRecommendation(
                "Stanford University",
                "Stanford, CA, USA",
                4,
                56000,
                "Full need-based financial aid, merit scholarships available",
                hasStrongExtracurriculars ? 70 : 55,
                "Engineering, Computer Science, Business, Environmental Science",
                "Regular Decision: January 5, Early Action: November 1"
            ));
        }

        // Strong universities (selective)
        if ((gpa != null && gpa >= 3.5) && 
            ((satScore != null && satScore >= 1400) || (actScore != null && actScore >= 31))) {
            
            recommendations.add(new CollegeRecommendation(
                "University of Michigan",
                "Ann Arbor, MI, USA",
                23,
                49000,
                "Merit scholarships and need-based aid available",
                hasStrongExtracurriculars ? 85 : 70,
                "Engineering, Business, Psychology, Computer Science",
                "Regular Decision: February 1, Early Action: November 1"
            ));
            
            recommendations.add(new CollegeRecommendation(
                "Boston University",
                "Boston, MA, USA",
                20,
                58000,
                "Merit scholarships up to $25,000 per year",
                hasStrongExtracurriculars ? 80 : 65,
                "Business, Communications, Engineering, Life Sciences",
                "Regular Decision: January 4, Early Decision: November 1"
            ));
        }

        // Good universities (moderate selectivity)
        if ((gpa != null && gpa >= 3.0) && 
            ((satScore != null && satScore >= 1200) || (actScore != null && actScore >= 25))) {
            
            recommendations.add(new CollegeRecommendation(
                "Penn State University",
                "University Park, PA, USA",
                56,
                35000,
                "Various merit and need-based scholarships available",
                hasStrongExtracurriculars ? 90 : 80,
                "Engineering, Business, Agriculture, Liberal Arts",
                "Rolling Admissions: Priority by November 30"
            ));
            
            recommendations.add(new CollegeRecommendation(
                "Arizona State University",
                "Tempe, AZ, USA",
                88,
                29000,
                "New American University Scholars Program, merit awards",
                hasStrongExtracurriculars ? 95 : 85,
                "Business, Engineering, Journalism, Sciences",
                "Rolling Admissions: Priority by February 1"
            ));
        }

        // International universities (if interested)
        if (countries != null && !countries.isEmpty()) {
            if (countries.contains("UK") && ieltsScore != null && ieltsScore >= 6.5) {
                recommendations.add(new CollegeRecommendation(
                    "University of Manchester",
                    "Manchester, UK",
                    56,
                    25000,
                    "International Excellence Scholarships available",
                    hasStrongExtracurriculars ? 75 : 65,
                    "Engineering, Business, Sciences, Humanities",
                    "UCAS Deadline: January 15"
                ));
            }
            
            if (countries.contains("Canada") && ieltsScore != null && ieltsScore >= 6.0) {
                recommendations.add(new CollegeRecommendation(
                    "University of Toronto",
                    "Toronto, Canada",
                    43,
                    45000,
                    "Lester B. Pearson International Scholarship",
                    hasStrongExtracurriculars ? 80 : 70,
                    "Computer Science, Engineering, Life Sciences, Arts",
                    "Regular Decision: January 15"
                ));
            }
        }

        // Add scholarship-focused options if financial aid is needed
        if (financialState != null && 
            (financialState.toLowerCase().contains("need") || 
             financialState.toLowerCase().contains("scholarship"))) {
            
            recommendations.add(new CollegeRecommendation(
                "University of Alabama",
                "Tuscaloosa, AL, USA",
                80,
                30000,
                "Presidential Scholarship, full tuition for high achievers",
                hasStrongExtracurriculars ? 95 : 85,
                "Business, Engineering, Communications, Sciences",
                "Rolling Admissions: Priority by December 15"
            ));
        }

        return recommendations;
    }

    private boolean hasStrongExtracurriculars(ExtracurricularInfoDto info) {
        if (info == null) return false;
        
        int score = 0;
        
        // Check leadership roles
        if (info.getLeadershipRoles() != null && !info.getLeadershipRoles().isEmpty()) {
            score += 2;
        }
        
        // Check awards
        if (info.getAwards() != null && !info.getAwards().isEmpty()) {
            score += 2;
        }
        
        // Check clubs
        if (info.getClubs() != null && !info.getClubs().isEmpty()) {
            score += 1;
        }
        
        // Check volunteer work
        if (info.getVolunteerWork() != null && !info.getVolunteerWork().isEmpty()) {
            score += 1;
        }
        
        return score >= 3; // Consider strong if score is 3 or higher
    }

    private static class CollegeRecommendation {
        private final String universityName;
        private final String location;
        private final int acceptanceRate;
        private final int annualTuition;
        private final String scholarships;
        private final int probability;
        private final String programs;
        private final String deadlines;

        public CollegeRecommendation(String universityName, String location, int acceptanceRate,
                                   int annualTuition, String scholarships, int probability,
                                   String programs, String deadlines) {
            this.universityName = universityName;
            this.location = location;
            this.acceptanceRate = acceptanceRate;
            this.annualTuition = annualTuition;
            this.scholarships = scholarships;
            this.probability = probability;
            this.programs = programs;
            this.deadlines = deadlines;
        }

        // Getters
        public String getUniversityName() { return universityName; }
        public String getLocation() { return location; }
        public int getAcceptanceRate() { return acceptanceRate; }
        public int getAnnualTuition() { return annualTuition; }
        public String getScholarships() { return scholarships; }
        public int getProbability() { return probability; }
        public String getPrograms() { return programs; }
        public String getDeadlines() { return deadlines; }
    }
} 