package com.sequenceiq.it.verification;

import java.util.HashMap;
import java.util.Map;

public class Call {

    private String method;

    private String uri;

    private String contentType;

    private String postBody;

    private String url;

    private final Map<String, String> headers = new HashMap<>();

    private Map<String, String> parameters = new HashMap<>();

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
    public CharSequence getPostBody() {
        return postBody;
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
