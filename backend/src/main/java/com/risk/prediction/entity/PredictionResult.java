package com.risk.prediction.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prediction_result")
public class PredictionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stkcd;

    @Column(name = "predict_date")
    private LocalDateTime predictDate = LocalDateTime.now();

    @Column(name = "risk_score")
    private BigDecimal riskScore;

    @Column(name = "is_risk")
    private Boolean isRisk;

    @Column(name = "shap_values", columnDefinition = "TEXT")
    private String shapValues;

    @Column(name = "feature_values_json", columnDefinition = "TEXT")
    private String featureValuesJson;

    @Column(name = "risk_level")
    private String riskLevel;
}
