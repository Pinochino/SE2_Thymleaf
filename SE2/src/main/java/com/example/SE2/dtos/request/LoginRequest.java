package com.example.SE2.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public class LoginRequest {

    @Email(message = "Email is not valid format")
    @NotBlank(message = "Email is mandatory")
    private String email;


    @NotBlank(message = "Password is mandatory")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
