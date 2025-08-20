package com.ledger.ledger.repository;

import com.ledger.ledger.domain.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {

}
