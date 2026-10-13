package com.shop.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Giả lập gửi mail bằng cách ghi log.
 * Muốn gửi thật: thêm spring-boot-starter-mail và viết SmtpEmailSender implements EmailSender.
 */
@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        try {
            Thread.sleep(1500); // mô phỏng SMTP chậm - khách hàng KHÔNG phải chờ vì chạy ở consumer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("\n===== EMAIL =====\nTo: {}\nSubject: {}\n{}\n=================", to, subject, body);
    }
}
