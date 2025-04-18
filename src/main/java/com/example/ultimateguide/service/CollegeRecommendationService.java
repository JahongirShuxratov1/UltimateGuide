package com.example.ultimateguide.service;

import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.model.UniversityRecommendation;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ChatSession;
import com.google.cloud.vertexai.generativeai.preview.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CollegeRecommendationService {
    private final UserService userService;
    private final GenerativeModel model;
    private final ObjectMapper objectMapper;

    public byte[] generateRecommendations(Long telegramId) throws IOException {
        UserDto userDto = userService.getUserByTelegramId(telegramId);
        if (userDto == null) {
            throw new IllegalStateException("User not found");
        }

        List<UniversityRecommendation> recommendations = getAIRecommendations(userDto);
        return createExcelFile(recommendations);
    }

    private List<UniversityRecommendation> getAIRecommendations(UserDto user) {
        try {
            // First, get real-time university data
            String searchPrompt = buildSearchPrompt(user);
            ChatSession searchSession = model.startChat();
            GenerateContentResponse searchResponse = searchSession.sendMessage(searchPrompt);
            String universitiesData = ResponseHandler.getText(searchResponse);

            // Then, get personalized recommendations based on the data
            String recommendationPrompt = buildRecommendationPrompt(user, universitiesData);
            ChatSession recommendationSession = model.startChat();
            GenerateContentResponse recommendationResponse = recommendationSession.sendMessage(recommendationPrompt);
            String jsonResponse = ResponseHandler.getText(recommendationResponse);

            return objectMapper.readValue(jsonResponse, new TypeReference<List<UniversityRecommendation>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to get AI recommendations: " + e.getMessage(), e);
        }
    }

    private String buildSearchPrompt(UserDto user) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Search for real universities in ");
        
        if (user.getPersonalInfo() != null && user.getPersonalInfo().getCountriesOfInterest() != null 
            && !user.getPersonalInfo().getCountriesOfInterest().isEmpty()) {
            prompt.append(String.join(", ", user.getPersonalInfo().getCountriesOfInterest()));
        } else {
            prompt.append("worldwide");
        }
        
        prompt.append(" that offer ");
        
        if (user.getPersonalInfo() != null && user.getPersonalInfo().getMajor() != null) {
            prompt.append(user.getPersonalInfo().getMajor());
        } else {
            prompt.append("various programs");
        }
        
        prompt.append(". For each university, find:\n");
        prompt.append("1. Current acceptance rates\n");
        prompt.append("2. Annual tuition fees for international students\n");
        prompt.append("3. Available scholarships and financial aid\n");
        prompt.append("4. Specific programs related to the student's interests\n");
        prompt.append("5. Application deadlines for international students\n");
        prompt.append("6. Entry requirements including test scores and GPA\n");
        prompt.append("\nProvide real, up-to-date information from reliable sources.");
        
        return prompt.toString();
    }

    private String buildRecommendationPrompt(UserDto user, String universitiesData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following university data and student profile, recommend the best matching universities.\n\n");
        prompt.append("University Data:\n").append(universitiesData).append("\n\n");
        prompt.append("Student Profile:\n");
        
        // Academic Info
        if (user.getAcademicInfo() != null) {
            prompt.append("Academic Qualifications:\n");
            if (user.getAcademicInfo().getSatScore() != null) {
                prompt.append("- SAT Score: ").append(user.getAcademicInfo().getSatScore()).append("\n");
            }
            if (user.getAcademicInfo().getActScore() != null) {
                prompt.append("- ACT Score: ").append(user.getAcademicInfo().getActScore()).append("\n");
            }
            if (user.getAcademicInfo().getIeltsScore() != null) {
                prompt.append("- IELTS Score: ").append(user.getAcademicInfo().getIeltsScore()).append("\n");
            }
            if (user.getAcademicInfo().getGpa() != null) {
                prompt.append("- GPA: ").append(user.getAcademicInfo().getGpa()).append("\n");
            }
        }
        
        // Extracurricular Info
        if (user.getExtracurricularInfo() != null) {
            prompt.append("\nExtracurricular Activities:\n");
            if (!user.getExtracurricularInfo().getClubs().isEmpty()) {
                prompt.append("- Clubs: ").append(String.join(", ", user.getExtracurricularInfo().getClubs())).append("\n");
            }
            if (!user.getExtracurricularInfo().getLeadershipRoles().isEmpty()) {
                prompt.append("- Leadership: ").append(String.join(", ", user.getExtracurricularInfo().getLeadershipRoles())).append("\n");
            }
            if (!user.getExtracurricularInfo().getVolunteerWork().isEmpty()) {
                prompt.append("- Volunteer Work: ").append(String.join(", ", user.getExtracurricularInfo().getVolunteerWork())).append("\n");
            }
            if (!user.getExtracurricularInfo().getAwards().isEmpty()) {
                prompt.append("- Awards: ").append(String.join(", ", user.getExtracurricularInfo().getAwards())).append("\n");
            }
        }
        
        // Personal Info
        if (user.getPersonalInfo() != null) {
            prompt.append("\nPreferences:\n");
            if (user.getPersonalInfo().getMajor() != null) {
                prompt.append("- Desired Major: ").append(user.getPersonalInfo().getMajor()).append("\n");
            }
            if (user.getPersonalInfo().getCountriesOfInterest() != null && !user.getPersonalInfo().getCountriesOfInterest().isEmpty()) {
                prompt.append("- Preferred Countries: ").append(String.join(", ", user.getPersonalInfo().getCountriesOfInterest())).append("\n");
            }
            if (user.getPersonalInfo().getFinancialState() != null) {
                prompt.append("- Financial State: ").append(user.getPersonalInfo().getFinancialState()).append("\n");
            }
        }

        prompt.append("\nAnalyze the universities and provide recommendations in this JSON format:\n");
        prompt.append("[{\n");
        prompt.append("  \"universityName\": \"...\",\n");
        prompt.append("  \"location\": \"...\",\n");
        prompt.append("  \"acceptanceRate\": 0.XX,\n");
        prompt.append("  \"annualTuition\": XXXXX.XX,\n");
        prompt.append("  \"scholarships\": [\"...\", \"...\"],\n");
        prompt.append("  \"probability\": 0.XX,\n");
        prompt.append("  \"programs\": [\"...\", \"...\"],\n");
        prompt.append("  \"deadlines\": [\"...\", \"...\"]\n");
        prompt.append("}]\n\n");
        prompt.append("Consider these factors:\n");
        prompt.append("1. Academic match (compare student's scores with university requirements)\n");
        prompt.append("2. Program availability (match with desired major)\n");
        prompt.append("3. Financial fit (consider tuition, scholarships, and student's financial state)\n");
        prompt.append("4. Location preference (prioritize universities in preferred countries)\n");
        prompt.append("5. Extracurricular alignment (match student's activities with university strengths)\n");
        prompt.append("\nProvide only real universities with accurate, current information. Format numbers as decimals (e.g., 0.85 for 85%).");

        return prompt.toString();
    }

    private byte[] createExcelFile(List<UniversityRecommendation> recommendations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("College Recommendations");

            // Create header row with styling
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"University", "Location", "Acceptance Rate", "Annual Tuition", 
                              "Scholarships", "Admission Probability", "Recommended Programs", "Application Deadlines"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 256 * 30); // Set column width to 30 characters
            }

            // Create cell styles for data rows
            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
            
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0"));

            // Fill data rows
            int rowNum = 1;
            for (UniversityRecommendation rec : recommendations) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(rec.getUniversityName());
                row.createCell(1).setCellValue(rec.getLocation());
                
                Cell acceptanceCell = row.createCell(2);
                acceptanceCell.setCellValue(rec.getAcceptanceRate());
                acceptanceCell.setCellStyle(percentStyle);
                
                Cell tuitionCell = row.createCell(3);
                tuitionCell.setCellValue(rec.getAnnualTuition());
                tuitionCell.setCellStyle(currencyStyle);
                
                row.createCell(4).setCellValue(String.join("\n", rec.getScholarships()));
                
                Cell probCell = row.createCell(5);
                probCell.setCellValue(rec.getProbability());
                probCell.setCellStyle(percentStyle);
                
                row.createCell(6).setCellValue(String.join("\n", rec.getPrograms()));
                row.createCell(7).setCellValue(String.join("\n", rec.getDeadlines()));
            }

            // Enable auto-filter
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
} 