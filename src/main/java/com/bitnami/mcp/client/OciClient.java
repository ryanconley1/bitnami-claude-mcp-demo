package com.bitnami.mcp.client;

import com.bitnami.mcp.model.ChartsIndex;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Simplistic client tailored to charts-index consumption demos. Please do not reuse.
 */
@Repository
public class OciClient {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    // Possible list of chart-index files as per TAC pipeline conventions. We can try to introspect content
    // from all these paths
    public static final List<String> CHART_INDEX_FILES = List.of(
            "charts/debian-11/charts-index",
            "charts/debian-12/charts-index",
            "charts/photon-4/charts-index",
            "charts/photon-5/charts-index",
            "charts/redhatubi-8/charts-index",
            "charts/redhatubi-9/charts-index",
            "charts/ubuntu-20/charts-index",
            "charts/ubuntu-22/charts-index",
            "charts/ubuntu-24/charts-index"
    );
    private static final String CHARTS_INDEX_MEDIA_TYPE = "application/vnd.vmware.charts.index.layer.v1+json";

    private static final List<String> ALLOWED_OCI_MEDIA_TYPES = List.of(
            "application/vnd.docker.distribution.manifest.list.v2+json",
            "application/vnd.docker.distribution.manifest.v2+json",
            "application/vnd.oci.image.index.v1+json",
            "application/vnd.oci.image.manifest.v1+json"
    );

    //Default Values are useful for testing
    @Value("${OCI_REGISTRY:https://us-east1-docker.pkg.dev/vmw-app-catalog/hosted-registry-.......}")
    private String ociRegistry;

    @Value("${OCI_REGISTRY_USERNAME:_json_key_base64}")
    private String ociRegistryUsername;

    @Value("${OCI_REGISTRY_PASSWORD:ewogICJ0eXBlIjogInNlc..........}")
    private String ociRegistryPassword;

    @Value("${CHARTS_INDEX_PATH:}")
    private String chartsIndexPath;

    @Value("${CHARTS_INDEX_TAG:latest}")
    private String chartsIndexTag;

    private URI baseUrl;
    private HttpClient httpClient;

    @PostConstruct
    public void init() throws Exception {
        baseUrl = new URI(ociRegistry);
        final ConnectionKeepAliveStrategy keepAliveStrategy = new OciConnectionKeepAliveStrategy();

        final SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(30, TimeUnit.SECONDS)
                .build();

        final PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(50);
        poolingConnManager.setDefaultMaxPerRoute(10);
        poolingConnManager.setDefaultSocketConfig(socketConfig);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(30, TimeUnit.SECONDS)
                .setConnectionRequestTimeout(30, TimeUnit.SECONDS)
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnManager)
                .setKeepAliveStrategy(keepAliveStrategy)
                .disableCookieManagement()
                .build();
    }

    public ChartsIndex pullIndex() throws IOException, RuntimeException {

        ChartsIndex index = new ChartsIndex(new HashMap<>(), "v1");
        if (!StringUtils.isEmpty(chartsIndexPath)) {
            System.err.printf("Pulling charts index. [path=%s, tag=%s]", chartsIndexPath, chartsIndexTag);
            JsonNode manifest = pullManifest(chartsIndexPath, chartsIndexTag);
            index.append(chartsIndexPath, convertToChartIndex(chartsIndexPath, manifest));
        } else {
            // Try introspecting from common chart-index locations into a single charts index
            for (String path : CHART_INDEX_FILES) {
                if (existsManifest(path, chartsIndexTag)) {
                    System.err.printf("Found charts index. Pulling entries. [path=%s, tag=%s]", path, chartsIndexTag);
                    index.append(path, convertToChartIndex(path,
                            pullManifest(path, "latest")));
                }
            }
        }
        return index;
    }
    /**
     * Fetch the manifest identified by name and reference where reference can be a tag or digest.
     */
    public JsonNode pullManifest(final String name, final String reference) {
        URI manifestUri = createURI("/%s/manifests/%s", name, reference);
        System.err.printf("Pulling manifest. [uri=%s]", manifestUri);
        try (final OciClientResponse response = sendRequest(
                "GET",
                manifestUri,
                Map.of("Accept", String.join(",", ALLOWED_OCI_MEDIA_TYPES))
        )) {
            if (response.body() == null) {
                throw new OciClientException("Could not pull charts index. Body is null");
            }
            validateResponse(response);
            String responseBody = EntityUtils.toString(response.body().response().getEntity());
            return objectMapper.readTree(responseBody);
        } catch (OciClientException oce) {
            throw oce;
        } catch (Exception e) {
            throw new OciClientException("Could not pull charts index", e);
        }
    }

    private ChartsIndex convertToChartIndex(String path, JsonNode manifest) throws IOException {
        String digest = null;
        for (JsonNode layer : manifest.get("layers")) {
            if (layer.get("mediaType").asText().equals(CHARTS_INDEX_MEDIA_TYPE)) {
                digest = layer.get("digest").asText();
                break;
            }
        }

        if (digest == null) {
            throw new OciClientException("No chart index found for " + chartsIndexPath);
        }

        JsonNode chartIndexBlob = pullBlob(path, digest);
        return objectMapper.treeToValue(chartIndexBlob, ChartsIndex.class);
    }

    private OciClientResponse sendRequest(final String method, final URI uri, final Map<String, String> headers) {
        return sendRequest(method, uri, headers, null);
    }

    private OciClientResponse sendRequest(final String method, final URI uri, final Map<String, String> headers,
                                          final HttpEntity entity) {
        final ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(method).setUri(uri);
        headers.forEach(requestBuilder::addHeader);
        requestBuilder.setEntity(entity);

        final String usernamePassword = ociRegistryUsername + ":" + ociRegistryPassword;
        final String headerValue = "Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8));
        requestBuilder.addHeader("Authorization", headerValue);

        try {
            final HttpResponse response = httpClient.executeOpen(null, requestBuilder.build(), null);
            return mapResponse(new OciClientRequest(method, uri), response);
        } catch (final IOException e) {
            throw new OciClientException("Error sending a request in the OCI client. [method=%s, uri=%s]".formatted(method, uri), e);
        }
    }

    URI createURI(final String path, final String... args) {
        final String pathWithArgs = args.length > 0 ? path.formatted((Object[]) args) : path;

        String uri = baseUrl.toString();
        if (!uri.contains("/v2")) {
            String contextPath = baseUrl.getPath();
            uri = StringUtils.removeEnd(uri, contextPath) + "/v2" + contextPath;
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }

        if (pathWithArgs.startsWith("/")) {
            uri += pathWithArgs.substring(1);
        } else {
            uri += pathWithArgs;
        }

        return URI.create(uri);
    }

    private OciClientResponse mapResponse(final OciClientRequest request, final HttpResponse response) {
        final Map<String, List<String>> headers = new HashMap<>();
        for (final Header header : response.getHeaders()) {
            final String key = header.getName().toLowerCase(Locale.ROOT);
            headers.computeIfAbsent(key, k -> new ArrayList<>());
            headers.get(key).add(header.getValue());
        }

        return new OciClientResponse(request, response.getCode(), Collections.unmodifiableMap(headers),
                OciClientResponse.Body.create((ClassicHttpResponse) response));
    }
    void validateResponse(final OciClientResponse response) {
        if (response.status() > 299 && !(response.request().method().equals("HEAD") && response.status() == 404)) {
            try (response) {
                //TODO: Should handle retriable errors
                throw OciClientException.errorStatus(response);
            }
        }
    }

    public Boolean existsManifest(final String name, final String reference) {
        URI manifestUri = createURI("/%s/manifests/%s", name, reference);
        System.err.printf("Checking if manifest exists. [uri=%s]", manifestUri);
        try (final OciClientResponse response = sendRequest(
                "HEAD",
                manifestUri,
                Map.of("Accept", String.join(",", ALLOWED_OCI_MEDIA_TYPES))
        )) {
            return response.status() == HttpStatus.SC_OK;
        } catch (Exception e) {
            System.err.printf("Could not send HEAD request to manifest. [error=%s]", e.getMessage());
            return false;
        }
    }

    public JsonNode pullBlob(final String name, final String digest) {
        URI blobUri = createURI("/%s/blobs/%s", name, digest);
        System.err.printf("Pulling blob. [uri=%s]", blobUri);
        try (final OciClientResponse response = sendRequest(
                "GET",
                blobUri,
                Map.of()
        )) {
            if (response.body() == null) {
                throw new OciClientException("Could not pull charts index blob. Body is null");
            }
            validateResponse(response);
            String responseBody = EntityUtils.toString(response.body().response().getEntity());
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new OciClientException("Could not pull charts index blob", e);
        }
    }
}
