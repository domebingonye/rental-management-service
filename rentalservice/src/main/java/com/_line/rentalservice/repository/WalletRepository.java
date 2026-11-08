package com._line.rentalservice.repository;

import com._line.rentalservice.model.WalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    Optional<WalletEntity> findByUserUsername(String username);

    /**
     * Row lock (SELECT ... WITH (UPDLOCK, ROWLOCK) on SQL Server) held until the transaction commits,
     * so two concurrent fundings of the same wallet are applied one after the other, never lost.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.user.username = :username")
    Optional<WalletEntity> findByUsernameForUpdate(@Param("username") String username);

    @EntityGraph(attributePaths = "user")
    Optional<WalletEntity> findByWalletNumber(String walletNumber);

    boolean existsByWalletNumber(String walletNumber);
}
