package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.model.MockBankTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MockBankTransferRepository extends JpaRepository<MockBankTransferEntity, Long> {

    Optional<MockBankTransferEntity> findBySessionId(String sessionId);
}
