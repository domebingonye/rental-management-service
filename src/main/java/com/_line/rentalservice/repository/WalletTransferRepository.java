package com._line.rentalservice.repository;

import com._line.rentalservice.model.WalletTransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransferEntity, Long> {

    @EntityGraph(attributePaths = {"senderWallet", "senderWallet.user", "recipientWallet", "recipientWallet.user"})
    Optional<WalletTransferEntity> findByReference(String reference);

    @EntityGraph(attributePaths = {"senderWallet", "senderWallet.user", "recipientWallet", "recipientWallet.user"})
    @Query("select t from WalletTransferEntity t " +
            "where t.senderWallet.user.username = :username or t.recipientWallet.user.username = :username " +
            "order by t.id desc")
    Page<WalletTransferEntity> findInvolving(@Param("username") String username, Pageable pageable);
}
