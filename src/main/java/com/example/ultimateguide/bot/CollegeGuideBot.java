package com.example.ultimateguide.bot;

import com.example.ultimateguide.dto.UserDto;
import com.example.ultimateguide.dto.AcademicInfoDto;
import com.example.ultimateguide.dto.ExtracurricularInfoDto;
import com.example.ultimateguide.dto.PersonalInfoDto;
import com.example.ultimateguide.entity.User;
import com.example.ultimateguide.entity.AcademicInfo;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import com.example.ultimateguide.entity.PersonalInfo;
import com.example.ultimateguide.bot.BotState;
import com.example.ultimateguide.model.UserState;
import com.example.ultimateguide.service.MessageService;
import com.example.ultimateguide.service.UserService;
import com.example.ultimateguide.service.UserStateService;
import com.example.ultimateguide.service.CollegeRecommendationService;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CollegeGuideBot extends TelegramLongPollingBot {

    private final MessageService messageService;
    private final UserStateService userStateService;
    private final UserService userService;
    private final CollegeRecommendationService collegeRecommendationService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            if (update.getMessage().hasContact()) {
                handleContact(update.getMessage().getChatId(), update.getMessage().getContact());
            } else if (update.getMessage().hasDocument()) {
                handleDocument(update.getMessage().getChatId(), update.getMessage().getDocument());
            } else if (update.getMessage().hasText()) {
                handleMessage(update.getMessage().getChatId(), update.getMessage().getText());
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackData = callbackQuery.getData();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        try {
            // Check if user is registered
            if (!userService.isUserRegistered(chatId) && !callbackData.equals("MAIN_MENU")) {
                execute(messageService.createPhoneNumberMessage(chatId,
                    "Please complete registration first by sharing your phone number."));
                return;
            }

            // Initialize user object if registered (except for MAIN_MENU)
            User user = null;
            if (!callbackData.equals("MAIN_MENU")) {
                user = getUpdatedUser(chatId);
            }

            switch (callbackData) {
                case "VIEW_PROFILE":
                    userStateService.setUserState(chatId, BotState.VIEW_PROFILE);
                    // Get fresh user data
                    user = getUpdatedUser(chatId);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "Your Profile Status:\n" +
                        "Complete all sections to get college recommendations!\n" +
                        "\nTip: You can go back anytime using the Back button.",
                        createProfileMenuKeyboard(user)));
                    break;

                case "ACADEMIC_INFO":
                    userStateService.setUserState(chatId, BotState.ACADEMIC_INFO);
                    // Get fresh user data
                    user = getUpdatedUser(chatId);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "📚 Academic Information:\n\n" +
                        "• SAT Score (400-1600) or\n" +
                        "• ACT Score (1-36)\n" +
                        "• IELTS Score (0-9)\n" +
                        "• GPA (0-4.0)",
                        createAcademicSubMenu(user)));
                    break;

                case "EXTRACURRICULAR_INFO":
                    userStateService.setUserState(chatId, BotState.EXTRACURRICULAR_INFO);
                    // Get fresh user data
                    user = getUpdatedUser(chatId);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "🌟 Extracurricular Activities:\n\n" +
                        "• Clubs & Organizations\n" +   
                        "• Leadership Positions\n" +
                        "• Volunteer Work\n" +
                        "• Awards & Achievements",
                        createExtracurricularSubMenu(user)));
                    break;

                case "PERSONAL_INFO":
                    userStateService.setUserState(chatId, BotState.PERSONAL_INFO);
                    // Get fresh user data
                    user = getUpdatedUser(chatId);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "👤 Personal Information:\n\n" +
                        "• Intended Major\n" +
                        "• Countries of Interest\n" +
                        "• Financial Situation",
                        createPersonalSubMenu(user)));
                    break;

                case "MAIN_MENU":
                    userStateService.setUserState(chatId, BotState.MAIN_MENU);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "Main Menu:",
                        messageService.createMainMenuKeyboard()));
                    break;

                case "COLLEGE_RECOMMENDATIONS":
                    if (!isProfileComplete(user)) {
                        execute(messageService.createEditMessage(chatId, messageId,
                            "Please complete your profile first to get college recommendations.",
                            createProfileMenuKeyboard(user)));
                        return;
                    }
                    
                    try {
                        byte[] excelFile = collegeRecommendationService.generateRecommendations(chatId);
                        
                        // Send a message first
                        execute(messageService.createMessage(chatId,
                            "Generating your personalized college recommendations...",
                            null));
                        
                        // Send the Excel file
                        SendDocument sendDocument = new SendDocument();
                        sendDocument.setChatId(String.valueOf(chatId));
                        sendDocument.setDocument(new InputFile(new ByteArrayInputStream(excelFile), 
                            "College_Recommendations.xlsx"));
                        execute(sendDocument);
                        
                        // Send follow-up message with main menu
                        execute(messageService.createMessage(chatId,
                            "Here are your personalized college recommendations based on your profile! " +
                            "The Excel file contains detailed information about each recommended university.",
                            messageService.createMainMenuKeyboard()));
                    } catch (Exception e) {
                        execute(messageService.createEditMessage(chatId, messageId,
                            "Sorry, there was an error generating your recommendations. Please try again later.",
                            messageService.createMainMenuKeyboard()));
                    }
                    break;

                case "ESSAY_HELPER":
                    userStateService.setUserState(chatId, BotState.WAITING_FOR_ESSAY);
                    execute(messageService.createEditMessage(chatId, messageId,
                        "Please send your essay as a .docx or .txt file. I'll review it and provide feedback with suggestions in parentheses.",
                        createBackButton("MAIN_MENU")));
                    break;

                default:
                    handleSubMenuCallbacks(chatId, messageId, callbackData);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            try {
                execute(messageService.createMessage(chatId,
                    "An error occurred. Please try again.",
                    messageService.createMainMenuKeyboard()));
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private User getUpdatedUser(Long telegramId) {
        UserDto userDto = userService.getUserByTelegramId(telegramId);
        if (userDto == null) {
            throw new IllegalStateException("User not found with telegram ID: " + telegramId);
        }
        return convertDtoToUser(userDto);
    }

    private InlineKeyboardMarkup createAcademicSubMenu(User user) {
        // Always get fresh user data
        user = getUpdatedUser(user.getTelegramId());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        AcademicInfo academicInfo = user.getAcademicInfo();
        boolean hasSat = academicInfo != null && academicInfo.getSatScore() != null;
        boolean hasAct = academicInfo != null && academicInfo.getActScore() != null;
        boolean hasIelts = academicInfo != null && academicInfo.getIeltsScore() != null;
        boolean hasGpa = academicInfo != null && academicInfo.getGpa() != null;

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("SAT Score " + (hasSat ? "✅" : ""), "ENTER_SAT"));
        row1.add(createButton("ACT Score " + (hasAct ? "✅" : ""), "ENTER_ACT"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("IELTS Score " + (hasIelts ? "✅" : ""), "ENTER_IELTS"));
        row2.add(createButton("GPA " + (hasGpa ? "✅" : ""), "ENTER_GPA"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("⬅️ Back to Profile", "VIEW_PROFILE"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private InlineKeyboardMarkup createExtracurricularSubMenu(User user) {
        // Always get fresh user data
        user = getUpdatedUser(user.getTelegramId());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        ExtracurricularInfo ecInfo = user.getExtracurricularInfo();
        boolean hasClubs = ecInfo != null && ecInfo.getClubs() != null && !ecInfo.getClubs().isEmpty();
        boolean hasLeadership = ecInfo != null && ecInfo.getLeadershipRoles() != null && !ecInfo.getLeadershipRoles().isEmpty();
        boolean hasVolunteer = ecInfo != null && ecInfo.getVolunteerWork() != null && !ecInfo.getVolunteerWork().isEmpty();
        boolean hasAwards = ecInfo != null && ecInfo.getAwards() != null && !ecInfo.getAwards().isEmpty();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("Clubs " + (hasClubs ? "✅" : ""), "ADD_CLUB"));
        row1.add(createButton("Leadership " + (hasLeadership ? "✅" : ""), "ADD_LEADERSHIP"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("Volunteer Work " + (hasVolunteer ? "✅" : ""), "ADD_VOLUNTEER"));
        row2.add(createButton("Awards " + (hasAwards ? "✅" : ""), "ADD_AWARD"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("⬅️ Back to Profile", "VIEW_PROFILE"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private InlineKeyboardMarkup createPersonalSubMenu(User user) {
        // Always get fresh user data
        user = getUpdatedUser(user.getTelegramId());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        PersonalInfo personalInfo = user.getPersonalInfo();
        boolean hasMajor = personalInfo != null && personalInfo.getMajor() != null;
        boolean hasCountries = personalInfo != null && personalInfo.getCountriesOfInterest() != null && !personalInfo.getCountriesOfInterest().isEmpty();
        boolean hasFinancialState = personalInfo != null && personalInfo.getFinancialState() != null;

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("Major " + (hasMajor ? "✅" : ""), "ENTER_MAJOR"));
        row1.add(createButton("Countries " + (hasCountries ? "✅" : ""), "ADD_COUNTRY"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("Financial State " + (hasFinancialState ? "✅" : ""), "ENTER_FINANCIAL_STATE"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("⬅️ Back to Profile", "VIEW_PROFILE"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        text = text.replace("✓", "✅");
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void handleContact(Long chatId, Contact contact) {
        try {
            // Check if phone number is valid
            if (contact.getPhoneNumber() == null || contact.getPhoneNumber().isEmpty()) {
                execute(messageService.createMessage(chatId,
                    "Invalid phone number. Please try again.",
                    messageService.createPhoneNumberKeyboard()));
                return;
            }

            // Check if user already exists
            if (userService.isUserRegistered(chatId)) {
                execute(messageService.createMessage(chatId,
                    "You are already registered! Here's the main menu:",
                    messageService.createMainMenuKeyboard()));
                return;
            }

            // Check if phone number is already used
            if (userService.isPhoneNumberUsed(contact.getPhoneNumber())) {
                execute(messageService.createMessage(chatId,
                    "This phone number is already registered. Please use a different number.",
                    messageService.createPhoneNumberKeyboard()));
                return;
            }

            // Create new user with phone number
            User user = new User();
            user.setTelegramId(chatId);
            user.setPhoneNumber(contact.getPhoneNumber());
            user.setUsername(contact.getFirstName() + " " + contact.getLastName());

            try {
                userService.createUser(user);
                // Move to main menu
                userStateService.setUserState(chatId, BotState.MAIN_MENU);
                execute(messageService.createMessage(chatId,
                    "Thank you for registering! Here's the main menu:",
                    messageService.createMainMenuKeyboard()));
            } catch (Exception e) {
                execute(messageService.createMessage(chatId,
                    "Error during registration. Please try again later.",
                    messageService.createPhoneNumberKeyboard()));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Long chatId, String text) {
        BotState currentState = userStateService.getUserState(chatId);
        User updatedUser = null;

        try {
            if (text.equals("/start")) {
                // Check if user is already registered
                if (userService.isUserRegistered(chatId)) {
                    userStateService.setUserState(chatId, BotState.MAIN_MENU);
                    execute(messageService.createMessage(chatId,
                        "Welcome back! Here's the main menu:",
                        messageService.createMainMenuKeyboard()));
                    return;
                }

                userStateService.setUserState(chatId, BotState.WAITING_FOR_PHONE);
                execute(messageService.createMessage(chatId,
                    "Welcome to College Guide Bot! To get started, please share your phone number:",
                    messageService.createPhoneNumberKeyboard()));
                return;
            }

            // Check if user is registered before processing any other messages
            if (!userService.isUserRegistered(chatId)) {
                execute(messageService.createMessage(chatId,
                    "Please complete registration first by sharing your phone number.",
                    messageService.createPhoneNumberKeyboard()));
                return;
            }

            User user = getUpdatedUser(chatId);

            switch (currentState) {
                case WAITING_FOR_CLUB:
                    updateExtracurricularInfo(user, "club", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Club activity saved successfully! What else would you like to add?",
                        createExtracurricularSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_LEADERSHIP:
                    updateExtracurricularInfo(user, "leadership", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Leadership role saved successfully! What else would you like to add?",
                        createExtracurricularSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_VOLUNTEER:
                    updateExtracurricularInfo(user, "volunteer", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Volunteer work saved successfully! What else would you like to add?",
                        createExtracurricularSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_AWARD:
                    updateExtracurricularInfo(user, "award", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Award/Achievement saved successfully! What else would you like to add?",
                        createExtracurricularSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_SAT:
                    try {
                        int satScore = Integer.parseInt(text);
                        if (satScore >= 400 && satScore <= 1600) {
                            updateAcademicInfo(user, "sat", satScore);
                            // Get updated user data to show correct checkmarks
                            updatedUser = getUpdatedUser(chatId);
                            execute(messageService.createMessage(chatId,
                                "SAT score saved successfully! What else would you like to add?",
                                createAcademicSubMenu(updatedUser)));
                        } else {
                            execute(messageService.createMessage(chatId,
                                "Invalid SAT score. Please enter a number between 400 and 1600.",
                                createBackButton("ACADEMIC_INFO")));
                        }
                    } catch (NumberFormatException e) {
                        execute(messageService.createMessage(chatId,
                            "Please enter a valid number for SAT score.",
                            createBackButton("ACADEMIC_INFO")));
                    }
                    break;

                case WAITING_FOR_ACT:
                    try {
                        int actScore = Integer.parseInt(text);
                        if (actScore >= 1 && actScore <= 36) {
                            updateAcademicInfo(user, "act", actScore);
                            // Get updated user data to show correct checkmarks
                            updatedUser = getUpdatedUser(chatId);
                            execute(messageService.createMessage(chatId,
                                "ACT score saved successfully! What else would you like to add?",
                                createAcademicSubMenu(updatedUser)));
                        } else {
                            execute(messageService.createMessage(chatId,
                                "Invalid ACT score. Please enter a number between 1 and 36.",
                                createBackButton("ACADEMIC_INFO")));
                        }
                    } catch (NumberFormatException e) {
                        execute(messageService.createMessage(chatId,
                            "Please enter a valid number for ACT score.",
                            createBackButton("ACADEMIC_INFO")));
                    }
                    break;

                case WAITING_FOR_IELTS:
                    try {
                        double ieltsScore = Double.parseDouble(text);
                        if (ieltsScore >= 0.0 && ieltsScore <= 9.0) {
                            updateAcademicInfo(user, "ielts", ieltsScore);
                            // Get updated user data to show correct checkmarks
                            updatedUser = getUpdatedUser(chatId);
                            execute(messageService.createMessage(chatId,
                                "IELTS score saved successfully! What else would you like to add?",
                                createAcademicSubMenu(updatedUser)));
                        } else {
                            execute(messageService.createMessage(chatId,
                                "Invalid IELTS score. Please enter a number between 0.0 and 9.0.",
                                createBackButton("ACADEMIC_INFO")));
                        }
                    } catch (NumberFormatException e) {
                        execute(messageService.createMessage(chatId,
                            "Please enter a valid number for IELTS score.",
                            createBackButton("ACADEMIC_INFO")));
                    }
                    break;

                case WAITING_FOR_GPA:
                    try {
                        double gpa = Double.parseDouble(text);
                        if (gpa >= 0.0 && gpa <= 4.0) {
                            updateAcademicInfo(user, "gpa", gpa);
                            // Get updated user data to show correct checkmarks
                            updatedUser = getUpdatedUser(chatId);
                            execute(messageService.createMessage(chatId,
                                "GPA saved successfully! What else would you like to add?",
                                createAcademicSubMenu(updatedUser)));
                        } else {
                            execute(messageService.createMessage(chatId,
                                "Invalid GPA. Please enter a number between 0.0 and 4.0.",
                                createBackButton("ACADEMIC_INFO")));
                        }
                    } catch (NumberFormatException e) {
                        execute(messageService.createMessage(chatId,
                            "Please enter a valid number for GPA.",
                            createBackButton("ACADEMIC_INFO")));
                    }
                    break;

                case WAITING_FOR_MAJOR:
                    updatePersonalInfo(user, "major", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Major saved successfully! What else would you like to add?",
                        createPersonalSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_COUNTRY:
                    updatePersonalInfo(user, "country", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Country saved successfully! What else would you like to add?",
                        createPersonalSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_FINANCIAL_STATE:
                    updatePersonalInfo(user, "financial_state", text);
                    // Get updated user data to show correct checkmarks
                    updatedUser = getUpdatedUser(chatId);
                    execute(messageService.createMessage(chatId,
                        "Financial state saved successfully! What else would you like to add?",
                        createPersonalSubMenu(updatedUser)));
                    break;

                case WAITING_FOR_ESSAY:
                    execute(messageService.createMessage(chatId,
                        "Please send your essay as a .docx or .txt file.",
                        createBackButton("MAIN_MENU")));
                    break;

                default:
                    execute(messageService.createMessage(chatId,
                        "Please use the buttons to interact with the bot.",
                        messageService.createMainMenuKeyboard()));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            try {
                execute(messageService.createMessage(chatId,
                    "An error occurred. Please try again.",
                    messageService.createMainMenuKeyboard()));
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateAcademicInfo(User user, String field, Object value) {
        UserDto userDto = convertUserToDto(user);
        
        if (userDto.getAcademicInfo() == null) {
            userDto.setAcademicInfo(new AcademicInfoDto());
        }
        
        AcademicInfoDto academicInfo = userDto.getAcademicInfo();
        switch (field) {
            case "sat":
                academicInfo.setSatScore((Integer) value);
                break;
            case "act":
                academicInfo.setActScore((Integer) value);
                break;
            case "ielts":
                academicInfo.setIeltsScore((Double) value);
                break;
            case "gpa":
                academicInfo.setGpa((Double) value);
                break;
        }
        
        // Save and refresh user data
        userService.updateUser(user.getTelegramId(), userDto);
        // Clear any cached data
        user.setAcademicInfo(null);
    }

    private void updateExtracurricularInfo(User user, String field, String value) {
        UserDto userDto = convertUserToDto(user);
        
        if (userDto.getExtracurricularInfo() == null) {
            userDto.setExtracurricularInfo(new ExtracurricularInfoDto());
        }
        
        ExtracurricularInfoDto ecInfo = userDto.getExtracurricularInfo();
        switch (field) {
            case "club":
                if (ecInfo.getClubs() == null) {
                    ecInfo.setClubs(new ArrayList<>());
                }
                ecInfo.getClubs().add(value);
                break;
            case "leadership":
                if (ecInfo.getLeadershipRoles() == null) {
                    ecInfo.setLeadershipRoles(new ArrayList<>());
                }
                ecInfo.getLeadershipRoles().add(value);
                break;
            case "volunteer":
                if (ecInfo.getVolunteerWork() == null) {
                    ecInfo.setVolunteerWork(new ArrayList<>());
                }
                ecInfo.getVolunteerWork().add(value);
                break;
            case "award":
                if (ecInfo.getAwards() == null) {
                    ecInfo.setAwards(new ArrayList<>());
                }
                ecInfo.getAwards().add(value);
                break;
        }
        
        // Save and refresh user data
        userService.updateUser(user.getTelegramId(), userDto);
        // Clear any cached data
        user.setExtracurricularInfo(null);
    }

    private void updatePersonalInfo(User user, String field, String value) {
        UserDto userDto = convertUserToDto(user);
        
        if (userDto.getPersonalInfo() == null) {
            userDto.setPersonalInfo(new PersonalInfoDto());
        }
        
        PersonalInfoDto personalInfo = userDto.getPersonalInfo();
        switch (field) {
            case "major":
                personalInfo.setMajor(value);
                break;
            case "country":
                if (personalInfo.getCountriesOfInterest() == null) {
                    personalInfo.setCountriesOfInterest(new ArrayList<>());
                }
                personalInfo.getCountriesOfInterest().add(value);
                break;
            case "financial_state":
                personalInfo.setFinancialState(value);
                break;
        }
        
        // Save and refresh user data
        userService.updateUser(user.getTelegramId(), userDto);
        // Clear any cached data
        user.setPersonalInfo(null);
    }

    private void handleSubMenuCallbacks(Long chatId, Integer messageId, String callbackData) throws TelegramApiException {
        switch (callbackData) {
            case "ENTER_SAT":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_SAT);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your SAT score (400-1600):",
                    createBackButton("ACADEMIC_INFO")));
                break;

            case "ENTER_ACT":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_ACT);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your ACT score (1-36):",
                    createBackButton("ACADEMIC_INFO")));
                break;

            case "ENTER_IELTS":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_IELTS);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your IELTS score (0.0-9.0):",
                    createBackButton("ACADEMIC_INFO")));
                break;

            case "ENTER_GPA":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_GPA);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your GPA (0.0-4.0):",
                    createBackButton("ACADEMIC_INFO")));
                break;

            case "ADD_CLUB":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_CLUB);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your club or organization name:",
                    createBackButton("EXTRACURRICULAR_INFO")));
                break;

            case "ADD_LEADERSHIP":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_LEADERSHIP);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your leadership position:",
                    createBackButton("EXTRACURRICULAR_INFO")));
                break;

            case "ADD_VOLUNTEER":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_VOLUNTEER);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your volunteer work:",
                    createBackButton("EXTRACURRICULAR_INFO")));
                break;

            case "ADD_AWARD":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_AWARD);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your award or achievement:",
                    createBackButton("EXTRACURRICULAR_INFO")));
                break;

            case "ENTER_MAJOR":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_MAJOR);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter your intended major:",
                    createBackButton("PERSONAL_INFO")));
                break;

            case "ADD_COUNTRY":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_COUNTRY);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please enter a country you're interested in studying in:",
                    createBackButton("PERSONAL_INFO")));
                break;

            case "ENTER_FINANCIAL_STATE":
                userStateService.setUserState(chatId, BotState.WAITING_FOR_FINANCIAL_STATE);
                execute(messageService.createEditMessage(chatId, messageId,
                    "Please describe your financial situation (e.g., need full scholarship, can pay partial tuition, etc.):",
                    createBackButton("PERSONAL_INFO")));
                break;

            default:
                execute(messageService.createEditMessage(chatId, messageId,
                    "Invalid option selected. Please try again.",
                    createBackButton("VIEW_PROFILE")));
        }
    }

    private InlineKeyboardMarkup createBackButton(String backTo) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("⬅️ Back", backTo));
        
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private User convertDtoToUser(UserDto dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setTelegramId(dto.getTelegramId());
        user.setUsername(dto.getUsername());
        user.setPhoneNumber(dto.getPhoneNumber());
        
        if (dto.getAcademicInfo() != null) {
            AcademicInfo academicInfo = new AcademicInfo();
            academicInfo.setSatScore(dto.getAcademicInfo().getSatScore());
            academicInfo.setActScore(dto.getAcademicInfo().getActScore());
            academicInfo.setIeltsScore(dto.getAcademicInfo().getIeltsScore());
            academicInfo.setGpa(dto.getAcademicInfo().getGpa());
            academicInfo.setUser(user);
            user.setAcademicInfo(academicInfo);
        }
        
        if (dto.getExtracurricularInfo() != null) {
            ExtracurricularInfo extracurricularInfo = new ExtracurricularInfo();
            extracurricularInfo.setClubs(new ArrayList<>(dto.getExtracurricularInfo().getClubs()));
            extracurricularInfo.setLeadershipRoles(new ArrayList<>(dto.getExtracurricularInfo().getLeadershipRoles()));
            extracurricularInfo.setVolunteerWork(new ArrayList<>(dto.getExtracurricularInfo().getVolunteerWork()));
            extracurricularInfo.setAwards(new ArrayList<>(dto.getExtracurricularInfo().getAwards()));
            extracurricularInfo.setUser(user);
            user.setExtracurricularInfo(extracurricularInfo);
        }
        
        if (dto.getPersonalInfo() != null) {
            PersonalInfo personalInfo = new PersonalInfo();
            personalInfo.setMajor(dto.getPersonalInfo().getMajor());
            personalInfo.setCountriesOfInterest(new ArrayList<>(dto.getPersonalInfo().getCountriesOfInterest()));
            personalInfo.setFinancialState(dto.getPersonalInfo().getFinancialState());
            personalInfo.setUser(user);
            user.setPersonalInfo(personalInfo);
        }
        
        return user;
    }

    private UserDto convertUserToDto(User user) {
        if (user == null) return null;
        
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setTelegramId(user.getTelegramId());
        userDto.setUsername(user.getUsername());
        userDto.setPhoneNumber(user.getPhoneNumber());
        
        if (user.getAcademicInfo() != null) {
            AcademicInfoDto academicInfoDto = new AcademicInfoDto();
            academicInfoDto.setSatScore(user.getAcademicInfo().getSatScore());
            academicInfoDto.setActScore(user.getAcademicInfo().getActScore());
            academicInfoDto.setIeltsScore(user.getAcademicInfo().getIeltsScore());
            academicInfoDto.setGpa(user.getAcademicInfo().getGpa());
            userDto.setAcademicInfo(academicInfoDto);
        }
        
        if (user.getExtracurricularInfo() != null) {
            ExtracurricularInfoDto extracurricularInfoDto = new ExtracurricularInfoDto();
            extracurricularInfoDto.setClubs(user.getExtracurricularInfo().getClubs());
            extracurricularInfoDto.setLeadershipRoles(user.getExtracurricularInfo().getLeadershipRoles());
            extracurricularInfoDto.setVolunteerWork(user.getExtracurricularInfo().getVolunteerWork());
            extracurricularInfoDto.setAwards(user.getExtracurricularInfo().getAwards());
            userDto.setExtracurricularInfo(extracurricularInfoDto);
        }
        
        if (user.getPersonalInfo() != null) {
            PersonalInfoDto personalInfoDto = new PersonalInfoDto();
            personalInfoDto.setMajor(user.getPersonalInfo().getMajor());
            personalInfoDto.setCountriesOfInterest(user.getPersonalInfo().getCountriesOfInterest());
            personalInfoDto.setFinancialState(user.getPersonalInfo().getFinancialState());
            userDto.setPersonalInfo(personalInfoDto);
        }
        
        return userDto;
    }

    public InlineKeyboardMarkup createProfileMenuKeyboard(User user) {
        // Always get fresh user data
        user = getUpdatedUser(user.getTelegramId());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Check academic info completion
        boolean hasAcademicInfo = false;
        if (user != null && user.getAcademicInfo() != null) {
            AcademicInfo academicInfo = user.getAcademicInfo();
            hasAcademicInfo = academicInfo.getSatScore() != null || 
                            academicInfo.getActScore() != null ||
                            academicInfo.getIeltsScore() != null ||
                            academicInfo.getGpa() != null;
        }

        // Check extracurricular info completion
        boolean hasExtracurricularInfo = false;
        if (user != null && user.getExtracurricularInfo() != null) {
            ExtracurricularInfo ecInfo = user.getExtracurricularInfo();
            hasExtracurricularInfo = (ecInfo.getClubs() != null && !ecInfo.getClubs().isEmpty()) ||
                                   (ecInfo.getLeadershipRoles() != null && !ecInfo.getLeadershipRoles().isEmpty()) ||
                                   (ecInfo.getVolunteerWork() != null && !ecInfo.getVolunteerWork().isEmpty()) ||
                                   (ecInfo.getAwards() != null && !ecInfo.getAwards().isEmpty());
        }

        // Check personal info completion
        boolean hasPersonalInfo = false;
        if (user != null && user.getPersonalInfo() != null) {
            PersonalInfo personalInfo = user.getPersonalInfo();
            hasPersonalInfo = personalInfo.getMajor() != null ||
                            (personalInfo.getCountriesOfInterest() != null && !personalInfo.getCountriesOfInterest().isEmpty()) ||
                            personalInfo.getFinancialState() != null;
        }

        // Create buttons with verified emojis
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("📚 Academic Info " + (hasAcademicInfo ? "✅" : ""), "ACADEMIC_INFO"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🌟 Extracurricular Info " + (hasExtracurricularInfo ? "✅" : ""), "EXTRACURRICULAR_INFO"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("👤 Personal Info " + (hasPersonalInfo ? "✅" : ""), "PERSONAL_INFO"));

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("⬅️ Main Menu", "MAIN_MENU"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private boolean isProfileComplete(User user) {
        if (user == null) return false;

        // Check academic info
        boolean hasAcademicInfo = false;
        if (user.getAcademicInfo() != null) {
            hasAcademicInfo = user.getAcademicInfo().getSatScore() != null || 
                            user.getAcademicInfo().getActScore() != null ||
                            user.getAcademicInfo().getIeltsScore() != null ||
                            user.getAcademicInfo().getGpa() != null;
        }

        // Check extracurricular info
        boolean hasExtracurricularInfo = false;
        if (user.getExtracurricularInfo() != null) {
            hasExtracurricularInfo = (user.getExtracurricularInfo().getClubs() != null && !user.getExtracurricularInfo().getClubs().isEmpty()) ||
                                   (user.getExtracurricularInfo().getLeadershipRoles() != null && !user.getExtracurricularInfo().getLeadershipRoles().isEmpty()) ||
                                   (user.getExtracurricularInfo().getVolunteerWork() != null && !user.getExtracurricularInfo().getVolunteerWork().isEmpty()) ||
                                   (user.getExtracurricularInfo().getAwards() != null && !user.getExtracurricularInfo().getAwards().isEmpty());
        }

        // Check personal info
        boolean hasPersonalInfo = false;
        if (user.getPersonalInfo() != null) {
            hasPersonalInfo = user.getPersonalInfo().getMajor() != null ||
                            (user.getPersonalInfo().getCountriesOfInterest() != null && !user.getPersonalInfo().getCountriesOfInterest().isEmpty()) ||
                            user.getPersonalInfo().getFinancialState() != null;
        }

        return hasAcademicInfo && hasExtracurricularInfo && hasPersonalInfo;
    }

    private void handleDocument(Long chatId, Document document) {
        try {
            String fileName = document.getFileName().toLowerCase();
            
            // Check if the file is a supported format
            if (!fileName.endsWith(".docx") && !fileName.endsWith(".txt")) {
                execute(messageService.createMessage(chatId,
                    "Please send your essay as a .docx or .txt file.",
                    createBackButton("MAIN_MENU")));
                return;
            }
            
            // Get the file
            String fileId = document.getFileId();
            File file = execute(new GetFile(fileId));
            String filePath = file.getFilePath();
            
            // Download the file
            java.io.File downloadedFile = downloadTelegramFile(filePath);
            
            // Read the content
            String essayContent = "";
            if (fileName.endsWith(".docx")) {
                essayContent = readDocxFile(downloadedFile);
            } else if (fileName.endsWith(".txt")) {
                essayContent = readTextFile(downloadedFile);
            }
            
            // Analyze the essay and provide feedback
            String feedback = analyzeEssay(essayContent);
            
            // Create suggested version with improvements
            byte[] suggestedEssayBytes = createSuggestedEssay(essayContent);
            
            // Send the feedback message
            execute(messageService.createMessage(chatId,
                "Here's my feedback on your essay:\n\n" + feedback + "\n\nI'm also sending you a suggested version of your essay with improvements marked in comments.",
                null));
            
            // Send the suggested essay as a docx file
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(String.valueOf(chatId));
            sendDocument.setDocument(new InputFile(new ByteArrayInputStream(suggestedEssayBytes), "Suggested_Essay.docx"));
            execute(sendDocument);
            
            // Send final message with main menu
            execute(messageService.createMessage(chatId,
                "You can review the suggested changes in the document. Feel free to use what you find helpful!",
                messageService.createMainMenuKeyboard()));
            
            // Reset user state
            userStateService.setUserState(chatId, BotState.MAIN_MENU);
            
        } catch (Exception e) {
            try {
                execute(messageService.createMessage(chatId,
                    "Sorry, there was an error processing your essay. Please try again.",
                    messageService.createMainMenuKeyboard()));
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
        }
    }

    private java.io.File downloadTelegramFile(String filePath) throws TelegramApiException {
        return downloadFile(filePath, new java.io.File(System.getProperty("java.io.tmpdir"), "essay"));
    }

    private String readDocxFile(java.io.File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    private String readTextFile(java.io.File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String analyzeEssay(String essayContent) {
        // This is a simplified analysis - in a real implementation, you would use more sophisticated NLP
        StringBuilder feedback = new StringBuilder();
        
        // Check essay length
        int wordCount = essayContent.split("\\s+").length;
        if (wordCount < 250) {
            feedback.append("Your essay is quite short. (Consider expanding your ideas to reach at least 250 words for a more comprehensive essay.)\n\n");
        } else if (wordCount > 650) {
            feedback.append("Your essay is quite long. (Consider trimming it to focus on your most compelling points and stay within typical college essay length limits of 500-650 words.)\n\n");
        }
        
        // Check for personal voice
        if (!essayContent.contains("I ") && !essayContent.contains(" my ") && !essayContent.contains(" me ")) {
            feedback.append("Your essay could use more personal voice. (Consider using first-person perspective to make your essay more personal and engaging.)\n\n");
        }
        
        // Check for specific examples
        if (!essayContent.matches(".*\\d{4}.*") && !essayContent.matches(".*\\d{1,2}/\\d{1,2}/\\d{2,4}.*")) {
            feedback.append("Your essay could benefit from more specific examples. (Consider including dates, specific events, or concrete details to strengthen your narrative.)\n\n");
        }
        
        // Check for conclusion
        if (!essayContent.toLowerCase().contains("conclusion") && !essayContent.toLowerCase().contains("in conclusion")) {
            feedback.append("Your essay might need a stronger conclusion. (Consider adding a clear conclusion that ties your ideas together and leaves a lasting impression.)\n\n");
        }
        
        // Check for introduction
        if (!essayContent.toLowerCase().contains("introduction") && !essayContent.toLowerCase().contains("to begin")) {
            feedback.append("Your essay might need a stronger introduction. (Consider adding a clear introduction that hooks the reader and presents your main idea.)\n\n");
        }
        
        // If no specific feedback was generated
        if (feedback.length() == 0) {
            feedback.append("Your essay looks good overall! (Consider having a teacher or counselor review it as well for additional perspectives.)");
        }
        
        return feedback.toString();
    }

    private byte[] createSuggestedEssay(String originalContent) throws IOException {
        XWPFDocument document = new XWPFDocument();
        
        // Add original content
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(originalContent);
        
        // Add suggestions based on analysis
        document.createParagraph(); // Add blank line
        XWPFParagraph suggestionsPara = document.createParagraph();
        XWPFRun suggestionsRun = suggestionsPara.createRun();
        suggestionsRun.setBold(true);
        suggestionsRun.setText("Suggested Improvements:");
        
        // Check word count
        int wordCount = originalContent.split("\\s+").length;
        if (wordCount < 250) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            r.setColor("FF0000"); // Red color
            r.setText("Length: Consider expanding your ideas to reach at least 250 words. You could add more details about:");
            addBulletPoints(document, new String[]{
                "Specific examples that support your main points",
                "Personal experiences related to your topic",
                "The broader implications of your argument"
            });
        }
        
        // Check personal voice
        if (!originalContent.contains("I ") && !originalContent.contains(" my ") && !originalContent.contains(" me ")) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            r.setColor("FF0000");
            r.setText("Personal Voice: Add more personal perspective using phrases like:");
            addBulletPoints(document, new String[]{
                "\"In my experience...\"",
                "\"I believe...\"",
                "\"This made me realize...\""
            });
        }
        
        // Check for specific examples
        if (!originalContent.matches(".*\\d{4}.*") && !originalContent.matches(".*\\d{1,2}/\\d{1,2}/\\d{2,4}.*")) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            r.setColor("FF0000");
            r.setText("Specific Examples: Include concrete details such as:");
            addBulletPoints(document, new String[]{
                "Dates of significant events",
                "Specific numbers or statistics",
                "Names of people, places, or organizations"
            });
        }
        
        // Check for introduction
        if (!originalContent.toLowerCase().contains("introduction") && !originalContent.toLowerCase().contains("to begin")) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            r.setColor("FF0000");
            r.setText("Introduction: Consider adding a strong opening that:");
            addBulletPoints(document, new String[]{
                "Hooks the reader with an interesting fact or question",
                "Provides context for your essay",
                "Clearly states your main argument or thesis"
            });
        }
        
        // Check for conclusion
        if (!originalContent.toLowerCase().contains("conclusion") && !originalContent.toLowerCase().contains("in conclusion")) {
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            r.setColor("FF0000");
            r.setText("Conclusion: End with a strong conclusion that:");
            addBulletPoints(document, new String[]{
                "Summarizes your main points",
                "Connects back to your introduction",
                "Leaves the reader with something to think about"
            });
        }
        
        // Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        return out.toByteArray();
    }

    private void addBulletPoints(XWPFDocument document, String[] points) {
        for (String point : points) {
            XWPFParagraph p = document.createParagraph();
            p.setIndentationLeft(720); // 0.5 inch indent
            XWPFRun r = p.createRun();
            r.setText("• " + point);
        }
    }
} 