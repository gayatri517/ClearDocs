package com.cleardocs.repository.jpa;

import com.cleardocs.model.jpa.Document;
import com.cleardocs.statemachine.DocumentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByReferenceNumber(String referenceNumber);

    Page<Document> findBySubmitterId(Long submitterId, Pageable pageable);

    Page<Document> findByState(DocumentState state, Pageable pageable);

    @Query("SELECT d FROM Document d JOIN FETCH d.submitter s " +
           "WHERE (:state IS NULL OR d.state = :state) " +
           "AND (:submitterId IS NULL OR d.submitter.id = :submitterId) " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:from IS NULL OR d.createdAt >= :from) " +
           "AND (:to IS NULL OR d.createdAt <= :to) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> search(
        @Param("state") DocumentState state,
        @Param("submitterId") Long submitterId,
        @Param("documentType") String documentType,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT d.state, COUNT(d) FROM Document d GROUP BY d.state")
    List<Object[]> countByStateGrouped();

    @Query("SELECT d FROM Document d WHERE d.state IN :states AND d.submitter.id = :userId")
    List<Document> findByStatesAndSubmitter(
        @Param("states") List<DocumentState> states,
        @Param("userId") Long userId
    );
}
