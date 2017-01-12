package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;

public class YarnEndpoint {

    private String apiEndpoint;

    private final String contextRoot = YarnResourceConstants.CONTEXT_ROOT;

    private final String version = YarnResourceConstants.API_VERSION;

    private String path;

    public YarnEndpoint(
            String apiEndpoint, String path) {
        this.apiEndpoint = apiEndpoint;
        this.path = path;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public String getVersion() {
        return version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URL getFullEndpointUrl() throws MalformedURLException {
        StringBuffer sb = new StringBuffer();
        sb.append(removeLeadingAndTrailingSlash(getApiEndpoint()));
        sb.append("/");
        sb.append(removeLeadingAndTrailingSlash(getContextRoot()));
        sb.append("/");
        sb.append(removeLeadingAndTrailingSlash(getVersion()));
        sb.append("/");
        sb.append(removeLeadingAndTrailingSlash(getPath()));
        return new URL(sb.toString());
    }

    @VisibleForTesting
    public String removeLeadingAndTrailingSlash(String s) {
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
