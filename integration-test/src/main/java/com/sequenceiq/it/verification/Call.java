package com.sequenceiq.it.verification;

import java.util.HashMap;
import java.util.Map;

import spark.Request;

public class Call {

    private String method;
    private String uri;
    private String contentType;
    private String postBody;
    private String url;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();
    private Request request;

    private Call() {
    }

    /**
     * Factory method
     */
    public static Call fromRequest(final Request request) {
        Call call = new Call();

        call.request = request;
        call.method = request.requestMethod();
        call.uri = request.uri();
        call.contentType = request.contentType();
        call.url = request.url();

        for (String s : request.headers()) {
            call.headers.put(s, request.headers(s));
        }

        call.parameters = new HashMap<>(request.params());

        call.postBody = request.body();

        return call;
    }

    /**
     * URI of the call
     */
    public String getUri() {
        return uri;
    }

    /**
     * Content type of the call
     */
    public String getContentType() {
        return contentType;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Map of headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Http method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Map of parameters. All parameters considered as if they were multi-valued.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * In case of POST request - returns post body
     */
    public String getPostBody() {
        return postBody;
    }

    /**
     * Returns raw HTTP request
     */
    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "Call{"
                + "url='" + url + '\''
                + ", uri='" + uri + '\''
                + ", contentType='" + contentType + '\''
                + ", postBody='" + postBody + '\''
                + ", method='" + method + '\''
                + ", parameters=" + parameters
                + '}';
    }
}
