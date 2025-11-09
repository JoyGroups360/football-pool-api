package com.leon.ideas.auth.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Value("${mail.brevo.api-key}")
    private String apiKey;

    @Value("${mail.brevo.api-url}")
    private String apiUrl;

    @Value("${mail.brevo.sender-email}")
    private String senderEmail;

    @Value("${mail.brevo.sender-name}")
    private String senderName;

    @Value("${mail.dev-mode}")
    private boolean devMode;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendEmail(String toEmail, String code) {        
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail, "name", senderName),
                    "to", new Object[]{Map.of("email", toEmail)},
                    "subject", "Verification code - Football Pool",
                    "textContent", "Your temporary code is: " + code + ". Expires in 30 minutes."
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
        } catch (Exception e) {
            logger.warn("⚠️ Failed to send email via Brevo: {}", e.getMessage());
            logger.info("📧 Email not sent, but code is shown above for testing");
        }
    
    }
}