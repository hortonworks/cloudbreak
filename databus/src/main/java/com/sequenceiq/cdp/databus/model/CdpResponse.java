package com.sequenceiq.cdp.databus.model;

import static com.cloudera.cdp.ValidationUtils.checkNotNullAndThrow;
import static com.cloudera.cdp.ValidationUtils.checkStateAndThrow;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

public abstract class CdpResponse {

    /**
     * The name of the response header containing the request ID.
     */
    public static final String CDP_HEADER_REQUESTID = "x-altus-request-id";

    private Integer httpCode;

    private Map<String, List<String>> responseHeaders;

    /**
     * Gets the http code that was returned by the CDP server.
     * @return the http code
     */
    public int getHttpCode() {
        checkNotNullAndThrow(httpCode);
        return httpCode;
    }

    /**
     * Sets the http code that was returned by the CDP server. This
     * should be called only once and never outside the SDK internals.
     * @param httpCode the status code
     */
    public void setHttpCode(int httpCode) {
        checkStateAndThrow(this.httpCode == null);
        this.httpCode = httpCode;
    }

    /**
     * Gets the http response headers that were returned by the CDP server.
     * @return the response headers
     * @deprecated use getResponseHeaders instead
     */
    @Deprecated
    public Map<String, List<String>> getResponseHeaaders() {
        return getResponseHeaders();
    }

    /**
     * Gets the http response headers that were returned by the CDP server.
     * @return the response headers
     */
    public Map<String, List<String>> getResponseHeaders() {
        checkNotNullAndThrow(responseHeaders);
        return responseHeaders;
    }

    /**
     * Sets the http response headers that were returned by the CDP server.
     * This should be called only once and never outside the SDK internals.
     * @param responseHeaders the response headers
     */
    public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
        checkStateAndThrow(this.responseHeaders == null);
        this.responseHeaders = responseHeaders;
    }

    /**
     * Returns the CDP request ID. CDP request IDs can be used in the event a
     * service call isn't working as expected and you need to work with CDP
     * support to debug an issue.
     * @return The CDP request ID
     */
    public String getRequestId() {
        checkNotNullAndThrow(responseHeaders);
        List<String> values = responseHeaders.get(CDP_HEADER_REQUESTID);
        if (values == null) {
            return "unknown";
        }
        return Iterables.getOnlyElement(values);
    }
}
