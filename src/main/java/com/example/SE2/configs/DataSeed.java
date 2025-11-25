package com.example.SE2.configs;

import com.example.SE2.constants.RoleName;
import com.example.SE2.models.Role;
import com.example.SE2.models.User;
import com.example.SE2.repositories.RoleRepository;
import com.example.SE2.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataSeed implements CommandLineRunner {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        Role role = roleRepository.findRoleByName(RoleName.SUPER_ADMIN);

        List<Role> roleList = List.of(new Role(RoleName.SUPER_ADMIN),
                new Role(RoleName.MODERATOR),
                new Role(RoleName.USER));

        if (role == null) {
            roleRepository.saveAll(roleList);
        }

        Set<Role> roles = new HashSet<>();
        Role superAdmin = roleRepository.findRoleByName(RoleName.SUPER_ADMIN);

        if (superAdmin == null) {
            throw new RuntimeException("Super admin not found");
        }

        roles.add(superAdmin);

        String adminEmail = "admin@gmail.com";

        User user = userRepository.findUserByEmail(adminEmail);

        if (user == null) {
            user = User.builder()
                    .fistName("John")
                    .lastName("Doe")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("123456"))
                    .phone("123456789")
                    .roles(roles)
                    .build();
            user = userRepository.save(user);
        }

        log.info("Admin created: " + user.getEmail());
    }
}
