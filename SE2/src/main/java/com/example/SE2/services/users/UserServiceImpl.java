package com.example.SE2.services.users;

import com.example.SE2.constants.Provider;
import com.example.SE2.models.User;
import com.example.SE2.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    UserRepository userRepository;

    @Override
    public void processOAuthPostLogin(String email, String firstName) {
        User user = userRepository.findUserByEmailOrFirstName(email, firstName) ;

        if(user == null){
            user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .provider(Provider.GOOGLE)
                    .build();
            userRepository.save(user);
        }
    }

}
