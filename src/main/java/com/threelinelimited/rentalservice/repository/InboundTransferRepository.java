package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.model.InboundTransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InboundTransferRepository extends JpaRepository<InboundTransferEntity, Long> {

    @EntityGraph(attributePaths = {"virtualAccount", "virtualAccount.wallet", "virtualAccount.wallet.user"})
    Optional<InboundTransferEntity> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"virtualAccount"})
    Page<InboundTransferEntity> findByVirtualAccountWalletUserUsernameOrderByIdDesc(String username, Pageable pageable);
}
