package com.leon.ideas.auth.service;

import com.leon.ideas.auth.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private AuthRepository authRepository;
    @Scheduled(fixedRate = 1800000)
    public void cleanExpiredResetCodes() {
        authRepository.deleteExpiredCodes();
    }
}






