package com.risk.prediction.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "company_info")
public class CompanyInfo {
    @Id
    private String stkcd;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    private String industry;

    @Column(name = "is_st")
    private Boolean isSt = false;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
