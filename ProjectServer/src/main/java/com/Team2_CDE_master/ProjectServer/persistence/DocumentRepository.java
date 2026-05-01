package com.Team2_CDE_master.ProjectServer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Spring Data JPA repository for DocumentEntity.
// JpaRepository gives us save(), findById(), findAll(), deleteById(), existsById() for free.
@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {
    // No custom queries needed — the inherited methods cover everything we use.
}
