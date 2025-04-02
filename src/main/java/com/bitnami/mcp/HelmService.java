package com.bitnami.mcp;

import com.bitnami.mcp.client.OciClient;
import com.bitnami.mcp.model.ApplicationMetadata;
import com.bitnami.mcp.model.ApplicationReadme;
import com.bitnami.mcp.model.ApplicationValues;
import com.bitnami.mcp.model.ApplicationVersion;
import com.bitnami.mcp.model.ChartsIndex;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HelmService {

    private final OciClient ociClient;

    private static final Logger log = LoggerFactory.getLogger(HelmService.class);

    private ChartsIndex chartsIndex;
    private final List<ApplicationMetadata> applications = new ArrayList<>();

    public HelmService(OciClient ociClient) {
        this.ociClient = ociClient;
    }

    @PostConstruct
    public void init()  {
    try {
        //String content = ResourceUtils.getText("classpath:charts-index.json");
        //chartsIndex = objectMapper.readValue(content, ChartsIndex.class);
        chartsIndex = ociClient.pullIndex();
        chartsIndex.entries().values().forEach(it -> {
            applications.add(it.metadata());
        });
        } catch (Exception e) {
            e.printStackTrace(); // Only then shows on Claude's log
            log.error("Could not load charts index", e);
        }
    }

    public ChartsIndex getChartsIndex() {
        return chartsIndex;
    }

    @Tool(name = "get_helm_charts", description = "Gets a list of all the Helm charts available in your Bitnami Premium or Tanzu Application Catalog subscription.")
    List<ApplicationMetadata> getHelmCharts() {
        return applications;
    }

    @Tool(name = "get_helm_chart", description = "Get a single Helm chart as long as it is available in your Bitnami Premium or Tanzu Application Catalog subscription.")
    ApplicationMetadata getHelmChart(String name) {
        return applications.stream().filter(it -> it.name().equals(name)).findFirst().orElse(null);
    }

    @Tool(name = "get_helm_chart_readme", description = "Returns the README content for any Helm chart from the Bitnami Premium or Tanzu Application Catalog subscription.")
    ApplicationReadme getHelmChartReadme(String name) {
        ApplicationMetadata metadata = getHelmChart(name);
        return new ApplicationReadme(metadata, readReadme(name).substring(0, 2000));
    }

    @Tool(name = "get_helm_chart_values", description = "Returns the default Helm chart values for any Helm chart from the Bitnami Premium or Tanzu Application Catalog subscription.")
    ApplicationValues getHelmChartValues(String name) {
        ApplicationMetadata metadata = getHelmChart(name);
        return new ApplicationValues(metadata, readValues(name));
    }


    @Tool(name = "get_helm_chart_versions", description = "Returns the different Helm chart versions to install available for a Helm chart in the Bitnami Premium or Tanzu Application Catalog subscription. These versions also contain the actual application version in the appVersion field. This app version is usually what the user understand better.")
    List<ApplicationVersion> getHelmChartVersions(String name) {
        if (chartsIndex.entries().get(name) == null) {
            return new ArrayList<>();
        }
        // Filter Helm charts to make the bot life's easier. We return only the oldest version per branch.
        Map<String, ApplicationVersion> mostRecentBranchVersions = new HashMap<>();
        chartsIndex.entries().get(name).versions().forEach(version -> {
            if (!mostRecentBranchVersions.containsKey(version.branch())) {
                mostRecentBranchVersions.put(version.branch(), version);
            } else if (mostRecentBranchVersions.get(version.branch()).releasedAt().before(version.releasedAt())) {
                mostRecentBranchVersions.put(version.branch(), version);
            }
        });
        return new ArrayList<ApplicationVersion>(mostRecentBranchVersions.values());
    }

    public String readReadme(String chart) {
        String fileUrl = String.format(
                "https://raw.githubusercontent.com/bitnami/charts/refs/heads/main/bitnami/%s/README.md",
                chart);
        try (InputStream inputStream = new URL(fileUrl).openStream()) {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            //return StringEscapeUtils.escapeJson(content);
            return content;
        } catch (IOException e) {
            log.error("Could not read chart readme", e);
            e.printStackTrace();
        }
        return "";
    }

    public String readValues(String chart) {
        String fileUrl = String.format(
                "https://raw.githubusercontent.com/bitnami/charts/refs/heads/main/bitnami/%s/values.yaml",
                chart);
        try (InputStream inputStream = new URL(fileUrl).openStream()) {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);;
            //return StringEscapeUtils.escapeJson(content);
            return content;
        } catch (IOException e) {
            log.error("Could not read chart default values", e);
            e.printStackTrace();
        }
        return "";
    }
}
