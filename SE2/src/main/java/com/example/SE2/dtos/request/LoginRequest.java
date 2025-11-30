package com.example.SE2.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @Email(message = "Email is not valid format")
    @NotBlank(message = "Email is mandatory")
    String email;


    @NotBlank(message = "Password is mandatory")
    String password;
}
