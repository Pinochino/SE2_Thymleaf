package com.example.SE2.dtos.request;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {


    String firstName;

    String lastName;

    String email;

    String password;

    String phone;

    String avatar;
}
