package com.sequenceiq.mock.verification;

import java.util.HashMap;
import java.util.Map;

public class Call {

    private String method;

    private String uri;

    private String contentType;

    private String postBody;

    private String url;

    private Map<String, String> headers = new HashMap<>();

    private Map<String, String> parameters = new HashMap<>();

    private Call() {
    }

    /**
     * Constructor for instances of type {@link Call} using the Builder implementation
     */
    private Call(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.contentType = builder.contentType;
        this.postBody = builder.postBody;
        this.url = builder.url;
        this.headers = builder.headers;
        this.parameters = builder.parameters;
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

    /**
     * Builder for instances of type {@link Call}
     */
    public static final class Builder {

        private String method;

        private String uri;

        private String contentType;

        private String postBody;

        private String url;

        private Map<String, String> headers;

        private Map<String, String> parameters;

        /**
         * Set the value of the field method of the target instance of type {@link Call}
         */
        public Builder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * Set the value of the field uri of the target instance of type {@link Call}
         */
        public Builder uri(final String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set the value of the field contentType of the target instance of type {@link Call}
         */
        public Builder contentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Set the value of the field postBody of the target instance of type {@link Call}
         */
        public Builder postBody(final String postBody) {
            this.postBody = postBody;
            return this;
        }

        /**
         * Set the value of the field url of the target instance of type {@link Call}
         */
        public Builder url(final String url) {
            this.url = url;
            return this;
        }

        /**
         * Set the value of the field headers of the target instance of type {@link Call}
         */
        public Builder headers(final Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Set the value of the field parameters of the target instance of type {@link Call}
         */
        public Builder parameters(final Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Create a new instance of type {@link Call}
         */
        public Call build() {
            return new Call(this);
        }
    }
}
