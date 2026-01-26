package com.example.SE2.dtos.request;

import java.util.Map;


public class MailRequest {
    private String from;
    private String to;
    private String subject;
    private String body;
    private Map<String, Object> model;
    private String templateName;

    public MailRequest(String from, String to, String subject, String body, Map<String, Object> model, String templateName) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.model = model;
        this.templateName = templateName;
    }

    public MailRequest() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
