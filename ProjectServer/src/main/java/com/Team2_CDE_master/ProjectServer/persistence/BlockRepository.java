package com.Team2_CDE_master.ProjectServer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// Spring Data JPA repository for BlockEntity.
@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, Long> {

    // Load all blocks for a document in their saved order (lowest ordering first)
    List<BlockEntity> findByDocumentIdOrderByOrdering(String documentId);

    // Delete all blocks for a document — used before re-saving to avoid duplicates
    // @Modifying makes Spring run this as a single bulk DELETE instead of row-by-row
    @Modifying
    @Query("DELETE FROM BlockEntity b WHERE b.documentId = :docId")
    void deleteByDocumentId(@Param("docId") String documentId);
}
