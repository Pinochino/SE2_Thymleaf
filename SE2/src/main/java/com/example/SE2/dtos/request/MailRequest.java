package com.example.SE2.dtos.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailRequest {
    String from;
    String to;
    String subject;
    String body;
    Map<String, Object> model;
    String templateName;
}
