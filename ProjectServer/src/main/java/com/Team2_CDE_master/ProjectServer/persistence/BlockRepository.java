package com.Team2_CDE_master.ProjectServer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, Long> {

    List<BlockEntity> findByDocumentIdOrderByOrdering(String documentId);

    @Modifying
    @Query("DELETE FROM BlockEntity b WHERE b.documentId = :docId")
    void deleteByDocumentId(@Param("docId") String documentId);
}
