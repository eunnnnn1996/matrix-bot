package com.example.bithumb.repository;

import com.example.bithumb.domain.BalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, Long> {
}
