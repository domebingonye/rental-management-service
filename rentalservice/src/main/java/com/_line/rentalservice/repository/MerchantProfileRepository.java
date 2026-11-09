package com._line.rentalservice.repository;

import com._line.rentalservice.enums.AccountStatus;
import com._line.rentalservice.model.MerchantProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantProfileRepository extends JpaRepository<MerchantProfileEntity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<MerchantProfileEntity> findByUserCode(String code);

    @EntityGraph(attributePaths = "user")
    List<MerchantProfileEntity> findByUserStatusOrderByIdAsc(AccountStatus status);
}
