package com.example.ultimateguide.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class ButtonService {

    public ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Register"));
        row1.add(new KeyboardButton("View Profile"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Get College Recommendations"));
        row2.add(new KeyboardButton("Essay Helper"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getAcademicInfoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Enter SAT Score"));
        row1.add(new KeyboardButton("Enter ACT Score"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Enter IELTS Score"));
        row2.add(new KeyboardButton("Enter GPA"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Back to Main Menu"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getExtracurricularInfoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Add Club"));
        row1.add(new KeyboardButton("Add Leadership Role"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Add Volunteer Work"));
        row2.add(new KeyboardButton("Add Award"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Back to Main Menu"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public ReplyKeyboardMarkup getPersonalInfoKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Enter Intended Major"));
        row1.add(new KeyboardButton("Add Country of Interest"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Financial Aid Needed"));
        row2.add(new KeyboardButton("No Financial Aid Needed"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Back to Main Menu"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }
} 