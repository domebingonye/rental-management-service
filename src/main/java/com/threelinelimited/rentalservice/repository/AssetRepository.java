package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.model.AssetEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    @EntityGraph(attributePaths = "merchant")
    Optional<AssetEntity> findByCode(String code);

    @EntityGraph(attributePaths = "merchant")
    Page<AssetEntity> findByMerchantUsernameOrderByIdDesc(String username, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssetEntity a where a.code = :code")
    Optional<AssetEntity> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);
}
