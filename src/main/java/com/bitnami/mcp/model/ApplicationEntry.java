package com.bitnami.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApplicationEntry(
        @JsonProperty("metadata") ApplicationMetadata metadata,
        @JsonProperty("versions") List<ApplicationVersion> versions) {
}
