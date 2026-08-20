package com.example.WebSideProject.repository;

import com.example.WebSideProject.entity.WeatherMailHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherMailHistoryRepository extends JpaRepository<WeatherMailHistory, Long> {
    List<WeatherMailHistory> findTop50ByOrderBySentAtDesc();
    List<WeatherMailHistory> findTop50ByUserEmailOrderBySentAtDesc(String userEmail);
}
