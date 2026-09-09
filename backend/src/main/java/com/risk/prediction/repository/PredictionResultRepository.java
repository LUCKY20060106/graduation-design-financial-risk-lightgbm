package com.risk.prediction.repository;

import com.risk.prediction.entity.PredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {
    List<PredictionResult> findByStkcd(String stkcd);
    void deleteByStkcd(String stkcd);
    List<PredictionResult> findAllByOrderByPredictDateDesc();
}
