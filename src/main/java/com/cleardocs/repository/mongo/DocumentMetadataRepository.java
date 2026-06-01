package com.cleardocs.repository.mongo;

import com.cleardocs.model.mongo.DocumentMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends MongoRepository<DocumentMetadata, String> {

    Optional<DocumentMetadata> findByDocumentId(Long documentId);

    Optional<DocumentMetadata> findByReferenceNumber(String referenceNumber);

    @Query("{ 'tags': { $in: ?0 } }")
    List<DocumentMetadata> findByTagsIn(List<String> tags);

    @Query("{ 'search_keywords': { $regex: ?0, $options: 'i' } }")
    List<DocumentMetadata> findBySearchKeyword(String keyword);

    void deleteByDocumentId(Long documentId);
}
