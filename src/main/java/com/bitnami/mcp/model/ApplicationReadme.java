package com.bitnami.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApplicationReadme(
        @JsonProperty("chart") ApplicationMetadata metadata,
        @JsonProperty("readme") String readme) {
}
