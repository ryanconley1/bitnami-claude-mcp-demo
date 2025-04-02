package com.bitnami.mcp.client;

import java.net.URI;

/**
 * This object represents a request made by {@link OciClient}.
 */
public record OciClientRequest(String method, URI uri) {

}
