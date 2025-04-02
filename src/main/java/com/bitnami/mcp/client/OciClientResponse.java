package com.bitnami.mcp.client;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This object represents a response given by {@link OciClient}.
 */
public record OciClientResponse(OciClientRequest request, int status, Map<String, List<String>> headers,
                                Body body)
        implements Closeable {

    @Override
    public void close() {
        if (this.body != null) {
            try {
                this.body.close();
            } catch (IOException ignored) {
                // NOOP
            }
        }
    }

    /**
     * It is the body of a response.
     */
    public record Body(HttpEntityContainer response) implements Closeable {

        @Override
        public void close() throws IOException {
            try {
                EntityUtils.consume(response.getEntity());
            } finally {
                if (response instanceof CloseableHttpResponse closeableHttpResponse) {
                    closeableHttpResponse.close();
                }
            }
        }

        /**
         * Creates a body from a {@link ClassicHttpResponse}.
         */
        public static Body create(final ClassicHttpResponse response) {
            if (response.getEntity() == null) {
                return null;
            } else {
                return new Body(response);
            }
        }

    }
}
