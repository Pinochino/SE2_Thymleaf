package com.example.SE2.models;

import com.example.SE2.constants.RoleName;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Role extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    RoleName name;

    public Role(RoleName name) {
        this.name = name;
    }

    @ManyToMany(mappedBy = "roles")
    @JsonBackReference
    Set<User> users = new HashSet<>();


}
