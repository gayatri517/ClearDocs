package com.cleardocs.model.mongo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "document_metadata")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentMetadata {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("document_id")
    private Long documentId;

    @Indexed
    @Field("reference_number")
    private String referenceNumber;

    @Field("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Field("custom_fields")
    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();

    @Field("version_history")
    @Builder.Default
    private List<VersionEntry> versionHistory = new ArrayList<>();

    @Field("comments")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Field("related_documents")
    @Builder.Default
    private List<Long> relatedDocuments = new ArrayList<>();

    @Field("search_keywords")
    @Builder.Default
    private List<String> searchKeywords = new ArrayList<>();

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VersionEntry {
        private Integer version;
        private String changedBy;
        private String changeDescription;
        private Instant changedAt;
        private String previousState;
        private String newState;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Comment {
        private String id;
        private String authorId;
        private String authorName;
        private String content;
        private Instant createdAt;
        private Boolean resolved;
    }
}
