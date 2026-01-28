package com.example.SE2.services.mail;

import com.example.SE2.dtos.request.MailRequest;

public interface MailService {
    void sendMail(MailRequest request);
}
