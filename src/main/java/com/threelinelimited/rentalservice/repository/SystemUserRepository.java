package com.threelinelimited.rentalservice.repository;

import com.threelinelimited.rentalservice.enums.UserRoleType;
import com.threelinelimited.rentalservice.model.SystemUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUserEntity, Long> {

    Optional<SystemUserEntity> findByUsername(String username);

    Optional<SystemUserEntity> findByUsernameOrEmail(String username, String email);

    Optional<SystemUserEntity> findByCode(String code);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCode(String code);

    boolean existsByUserRoleType(UserRoleType userRoleType);
}
