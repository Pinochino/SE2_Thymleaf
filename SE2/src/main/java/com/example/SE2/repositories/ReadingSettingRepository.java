package com.example.SE2.repositories;

import com.example.SE2.models.ReadingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingSettingRepository extends JpaRepository<ReadingSetting, Long> {
}
