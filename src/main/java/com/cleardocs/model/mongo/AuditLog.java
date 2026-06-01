package com.cleardocs.model.mongo;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "idx_audit_user_time", def = "{'user_id': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "idx_audit_resource_time", def = "{'resource_type': 1, 'resource_id': 1, 'timestamp': -1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private Long userId;

    @Field("username")
    private String username;

    @Field("action")
    private String action;

    @Field("resource_type")
    private String resourceType;

    @Field("resource_id")
    private String resourceId;

    @Field("description")
    private String description;

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    @Field("request_method")
    private String requestMethod;

    @Field("request_path")
    private String requestPath;

    @Field("response_status")
    private Integer responseStatus;

    @Field("execution_time_ms")
    private Long executionTimeMs;

    @Field("old_value")
    private Map<String, Object> oldValue;

    @Field("new_value")
    private Map<String, Object> newValue;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("success")
    @Builder.Default
    private Boolean success = true;

    @Field("error_message")
    private String errorMessage;

    @CreatedDate
    @Indexed
    @Field("timestamp")
    private Instant timestamp;
}
