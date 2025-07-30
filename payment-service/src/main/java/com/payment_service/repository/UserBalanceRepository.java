package com.payment_service.repository;

import com.payment_service.model.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, String> {

    /**
     * Finds a user's credit balance by their unique user ID.
     * @param userId The ID of the user.
     * @return An Optional containing the UserCredits if found.
     */
    Optional<UserBalance> findByUserId(String userId);
}