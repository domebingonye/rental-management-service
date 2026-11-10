package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.model.VirtualAccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccountEntity, Long> {

    @EntityGraph(attributePaths = {"wallet", "wallet.user"})
    Optional<VirtualAccountEntity> findByWalletUserUsername(String username);

    @EntityGraph(attributePaths = {"wallet", "wallet.user"})
    Optional<VirtualAccountEntity> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
