package com.example.ultimateguide.service;

import com.example.ultimateguide.bot.BotState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    private final Map<Long, BotState> userStates = new ConcurrentHashMap<>();

    public void setUserState(Long userId, BotState state) {
        userStates.put(userId, state);
    }

    public BotState getUserState(Long userId) {
        return userStates.getOrDefault(userId, BotState.START);
    }

    public void clearUserState(Long userId) {
        userStates.remove(userId);
    }
} 