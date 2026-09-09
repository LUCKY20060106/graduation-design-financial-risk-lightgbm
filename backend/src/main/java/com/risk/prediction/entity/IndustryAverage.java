package com.risk.prediction.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "industry_average")
public class IndustryAverage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String industryName;

    // 核心维度均值
    private BigDecimal avgSolvency;    // 偿债能力均值
    private BigDecimal avgOperation;   // 营运能力均值
    private BigDecimal avgGrowth;      // 发展能力均值
    private BigDecimal avgProfit;      // 盈利能力均值
    private BigDecimal avgCashFlow;    // 现金流水平均值
}
