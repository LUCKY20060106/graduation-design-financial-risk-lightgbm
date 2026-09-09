package com.risk.prediction.repository;

import com.risk.prediction.entity.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, String> {
}
