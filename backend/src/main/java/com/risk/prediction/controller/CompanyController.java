package com.risk.prediction.controller;

import com.risk.prediction.entity.CompanyInfo;
import com.risk.prediction.repository.CompanyInfoRepository;
import com.risk.prediction.repository.PredictionResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyInfoRepository companyInfoRepository;

    @Autowired
    private PredictionResultRepository predictionResultRepository;

    @GetMapping
    public List<CompanyInfo> getAllCompanies() {
        return companyInfoRepository.findAll();
    }

    @PostMapping
    public CompanyInfo saveCompany(@RequestBody CompanyInfo companyInfo) {
        if (companyInfo.getStkcd() == null || companyInfo.getStkcd().trim().isEmpty()) {
            throw new RuntimeException("股票代码不能为空");
        }
        return companyInfoRepository.save(companyInfo);
    }

    @DeleteMapping("/{stkcd}")
    @Transactional
    public Map<String, String> deleteCompany(@PathVariable String stkcd) {
        // 先删除关联的预测记录，否则由于外键约束会删除失败
        predictionResultRepository.deleteByStkcd(stkcd);
        companyInfoRepository.deleteById(stkcd);
        return Map.of("message", "删除成功");
    }
}
