package com.example.SE2.dtos.request;

import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    String firstName;

    String lastName;

    String email;

    String password;

    String phone;

    String avatar;
}
