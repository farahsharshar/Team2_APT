package com.Team2_CDE_master.ProjectServer.sharing;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Person C — Phase 3
 * Spring Data JPA repository for ShareCodeEntity.
 * Spring generates the implementation at runtime automatically.
 */
public interface ShareCodeRepository extends JpaRepository<ShareCodeEntity, String> {
    // Spring Data generates: findById, save, findAll, delete, etc.
    // No extra methods needed.
}
