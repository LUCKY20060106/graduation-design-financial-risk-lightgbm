package com.risk.prediction.repository;

import com.risk.prediction.entity.IndustryAverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IndustryAverageRepository extends JpaRepository<IndustryAverage, Long> {
    Optional<IndustryAverage> findByIndustryName(String industryName);
}
