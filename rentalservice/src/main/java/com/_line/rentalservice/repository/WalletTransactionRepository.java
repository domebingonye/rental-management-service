package com._line.rentalservice.repository;

import com._line.rentalservice.model.WalletTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactionEntity, Long> {

    Optional<WalletTransactionEntity> findByReference(String reference);

    Page<WalletTransactionEntity> findByWalletIdOrderByIdDesc(Long walletId, Pageable pageable);
}
