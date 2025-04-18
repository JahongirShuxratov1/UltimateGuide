package com.example.ultimateguide.service;

import com.example.ultimateguide.entity.User;
import com.example.ultimateguide.entity.AcademicInfo;
import com.example.ultimateguide.entity.ExtracurricularInfo;
import com.example.ultimateguide.entity.PersonalInfo;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {

    public SendMessage createMessage(Long chatId, String text, Object keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        
        if (keyboard instanceof InlineKeyboardMarkup) {
            message.setReplyMarkup((InlineKeyboardMarkup) keyboard);
        } else if (keyboard instanceof ReplyKeyboardMarkup) {
            message.setReplyMarkup((ReplyKeyboardMarkup) keyboard);
        }
        
        return message;
    }

    public EditMessageText createEditMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);
        message.setText(text);
        message.setReplyMarkup(keyboard);
        return message;
    }

    public ReplyKeyboardMarkup createPhoneNumberKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        KeyboardButton phoneButton = new KeyboardButton("Share Phone Number");
        phoneButton.setRequestContact(true);
        row.add(phoneButton);

        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        return keyboardMarkup;
    }

    public SendMessage createPhoneNumberMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(createPhoneNumberKeyboard());
        return message;
    }

    public InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("👤 View Profile", "VIEW_PROFILE"));
        row1.add(createButton("🎓 College Recommendations", "COLLEGE_RECOMMENDATIONS"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("✍️ Essay Helper", "ESSAY_HELPER"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createProfileMenuKeyboard(User user) {
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
        row1.add(createButton("📚 Academic Info " + (hasAcademicInfo ? "✓" : ""), "ACADEMIC_INFO"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🌟 Extracurricular Info " + (hasExtracurricularInfo ? "✓" : ""), "EXTRACURRICULAR_INFO"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("👤 Personal Info " + (hasPersonalInfo ? "✓" : ""), "PERSONAL_INFO"));

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("⬅️ Main Menu", "MAIN_MENU"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
} 