package com.bitnami.mcp.client;

public class OciClientException extends RuntimeException {

    /**
     * Constructor with message.
     */
    public OciClientException(final String message) {
        super(message);
    }

    /**
     * Constructor with message and cause.
     */
    public OciClientException(final String message, final Exception e) {
        super(message, e);
    }

    /**
     * Create a {@link OciClientException} with the proper message for a response with invalid HTTP status.
     */
    public static OciClientException errorStatus(final OciClientResponse response) {
        final OciClientRequest request = response.request();

        OciClientResponse.Body body = response.body();
        return new OciClientException(
                "Response with wrong status code. [method=%s, uri=%s, status=%d]".formatted(request.method(),
                        request.uri(), response.status())
        );
    }
}
