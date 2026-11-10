package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.model.RentalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<RentalEntity, Long> {

    @EntityGraph(attributePaths = {"asset", "customer", "merchant"})
    Optional<RentalEntity> findByReference(String reference);

    @EntityGraph(attributePaths = {"asset", "customer", "merchant"})
    Page<RentalEntity> findByCustomerUsernameOrderByIdDesc(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"asset", "customer", "merchant"})
    Page<RentalEntity> findByMerchantUsernameOrderByIdDesc(String username, Pageable pageable);
}
