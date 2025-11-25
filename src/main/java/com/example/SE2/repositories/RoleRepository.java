package com.example.SE2.repositories;

import com.example.SE2.constants.RoleName;
import com.example.SE2.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Role findRoleByName(RoleName name);
}
