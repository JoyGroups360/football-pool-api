package com.leon.ideas.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class GmailMailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailMailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    public GmailMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Set sender with custom name: "No Reply - Football Pool <email>"
            message.setFrom("No Reply - Football Pool <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject("Verification Code - Football Pool");
            message.setText("Your temporary code is: " + code + ". Expires in 30 minutes.");

            mailSender.send(message);
            
            logger.info("✅ Email sent successfully to: {} via Gmail SMTP", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send email to: {}. Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email via Gmail SMTP: " + e.getMessage(), e);
        }
    }
}

