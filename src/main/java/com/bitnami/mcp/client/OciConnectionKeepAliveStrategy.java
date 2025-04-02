package com.bitnami.mcp.client;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HeaderElements;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.util.concurrent.TimeUnit;

public class OciConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext httpContext) {

        BasicHeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HeaderElements.KEEP_ALIVE));
        while (it.hasNext()) {
            HeaderElement he = it.next();
            String param = he.getName();
            String value = he.getValue(); // Won't be null due to the iterator used
            if (param.equalsIgnoreCase("timeout")) {
                return TimeValue.of(Long.parseLong(value), TimeUnit.SECONDS);
            }
        }
        return TimeValue.of(5, TimeUnit.SECONDS);
    }
}