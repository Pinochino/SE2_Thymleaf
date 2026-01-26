package com.example.SE2.services.users;

import com.example.SE2.constants.Provider;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void processOAuthPostLogin(String email, String firstName) {
        User user = userRepository.findUserByEmailOrFirstName(email, firstName);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setProvider(Provider.GOOGLE);

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
