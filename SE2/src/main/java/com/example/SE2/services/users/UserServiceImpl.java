package com.example.SE2.services.users;

import com.example.SE2.constants.Provider;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public void processOAuthPostLogin(String email, String firstName) {
        User user = userRepository.findUserByEmailOrFirstName(email, firstName);

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .provider(Provider.GOOGLE)
                    .build();
            userRepository.save(user);
        }
    }

    @Override
    public void updateResetPasswordToken(String email, String token) {
        User user = userRepository.findUserByEmail(email);

        if (user != null) {
            user.setResetPasswordToken(token);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Couldn't find user by email");
        }
    }

    @Override
    public User getByResetPasswordToken(String token) {
        return userRepository.findUserByResetPasswordToken(token);
    }

    @Override
    public void updatePassword(User user, String token) {
        String encodedPassword = passwordEncoder.encode(token);
        user.setPassword(encodedPassword);

        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

}
