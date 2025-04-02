package com.bitnami.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApplicationMetadata(
        @JsonProperty("appKey") String name,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("description") String description,
        @JsonProperty("category") String category,
        @JsonProperty("tanzuCategory") String tanzuCategory) {
}