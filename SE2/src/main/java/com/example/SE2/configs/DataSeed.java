package com.example.SE2.configs;

import com.example.SE2.constants.RoleName;
import com.example.SE2.models.Role;
import com.example.SE2.models.User;
import com.example.SE2.repositories.RoleRepository;
import com.example.SE2.repositories.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataSeed implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(DataSeed.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataSeed(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


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
            user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail(adminEmail);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRoles(roles);
            user.setPhone("1234567890");

            user = userRepository.save(user);
        }

        log.info("Admin created: " + user.getEmail());
    }
}
