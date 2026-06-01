package com.cleardocs.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
public class DocumentRequest {
    @NotBlank @Size(max = 255)
    private String title;

    private String description;

    @NotBlank
    private String documentType;

    private String priority = "NORMAL";

    private List<String> tags;

    private Map<String, Object> customFields;

    private List<Long> reviewerIds;
}
