
package com.bitnami.mcp.model;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public record ChartsIndex(
        @JsonProperty("entries") Map<String, ApplicationEntry> entries,
        @JsonProperty("apiVersion") String apiVersion) {

    public ChartsIndex append(String path, ChartsIndex index) {

        int skipped = 0;
        int added = 0;
        for (Map.Entry<String, ApplicationEntry> entry : index.entries.entrySet()) {
            String appName = entry.getKey();
            if (entry.getValue().metadata() == null) {
                skipped++;
                continue;
            }
            if (this.entries.containsKey(appName)) {
                this.entries.get(appName).versions().addAll(entry.getValue().versions());
            } else {
                this.entries.put(appName, entry.getValue());
            }
            added++;
        }

        return this;
    }
}
