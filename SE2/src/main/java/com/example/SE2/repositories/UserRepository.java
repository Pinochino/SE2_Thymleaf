package com.example.SE2.repositories;

import com.example.SE2.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findUserByEmail(String email);

    @Query("select u from User u where u.isLoggedIn=true")
    List<User> findUsersByLoggedIn();
}
