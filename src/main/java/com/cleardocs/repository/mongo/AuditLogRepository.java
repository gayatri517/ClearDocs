package com.cleardocs.repository.mongo;

import com.cleardocs.model.mongo.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);

    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    Page<AuditLog> findByTimestampBetween(Instant from, Instant to, Pageable pageable);

    @Query("{ 'user_id': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<AuditLog> findByUserIdAndTimestampBetween(Long userId, Instant from, Instant to);

    @Query(value = "{ 'resource_type': 'DOCUMENT', 'resource_id': ?0 }", sort = "{ 'timestamp': -1 }")
    List<AuditLog> findDocumentAuditTrail(String documentId, Pageable pageable);

    long countByUserIdAndTimestampBetween(Long userId, Instant from, Instant to);
}
