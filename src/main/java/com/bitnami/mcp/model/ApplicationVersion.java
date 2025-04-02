package com.bitnami.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public record ApplicationVersion(
        @JsonProperty("version") String version,
        @JsonProperty("appVersion") String appVersion,
        @JsonProperty("name") String name,
        @JsonProperty("urls") List<String> urls,
        @JsonProperty("digest") String digest,
        @JsonProperty("releasedAt") Date releasedAt,
        @JsonProperty("releaseId") String releaseId,
        @JsonProperty("applicationId") String applicationId,
        @JsonProperty("branch") String branch,
        @JsonProperty("revision") String revision) {
}