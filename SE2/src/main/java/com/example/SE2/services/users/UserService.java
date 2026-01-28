package com.example.SE2.services.users;

import com.example.SE2.models.User;

public interface UserService
{
    void processOAuthPostLogin(String email, String firstName);

    void updateResetPasswordToken(String email, String token);

    User getByResetPasswordToken(String token);

    void updatePassword(User user, String token);
}
