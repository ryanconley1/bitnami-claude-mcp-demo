package com.bitnami.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApplicationValues(
        @JsonProperty("chart") ApplicationMetadata metadata,
        @JsonProperty("valuesYaml") String valuesYaml) {
}
