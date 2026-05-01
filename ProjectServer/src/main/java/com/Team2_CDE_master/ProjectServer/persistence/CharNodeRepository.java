package com.Team2_CDE_master.ProjectServer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// Spring Data JPA repository for CharNodeEntity.
@Repository
public interface CharNodeRepository extends JpaRepository<CharNodeEntity, Long> {

    // Load all char nodes for a specific block inside a specific document, in saved order
    List<CharNodeEntity> findByDocumentIdAndBlockSiteIdAndBlockCounterOrderByOrdering(
            String documentId, int blockSiteId, int blockCounter);

    // Delete all char nodes for a document — used before re-saving to avoid duplicates
    @Modifying
    @Query("DELETE FROM CharNodeEntity c WHERE c.documentId = :docId")
    void deleteByDocumentId(@Param("docId") String documentId);
}
