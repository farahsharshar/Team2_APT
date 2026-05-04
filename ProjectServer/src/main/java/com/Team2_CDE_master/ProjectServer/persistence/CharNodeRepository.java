package com.Team2_CDE_master.ProjectServer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharNodeRepository extends JpaRepository<CharNodeEntity, Long> {

    List<CharNodeEntity> findByDocumentIdAndBlockSiteIdAndBlockCounterOrderByOrdering(
            String documentId, int blockSiteId, int blockCounter);

    @Modifying
    @Query("DELETE FROM CharNodeEntity c WHERE c.documentId = :docId")
    void deleteByDocumentId(@Param("docId") String documentId);
}
