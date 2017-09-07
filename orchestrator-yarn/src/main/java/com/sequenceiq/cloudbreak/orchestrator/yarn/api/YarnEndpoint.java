package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.annotations.VisibleForTesting;

public class YarnEndpoint {

    private String apiEndpoint;

    private String path;

    public YarnEndpoint(String apiEndpoint, String path) {
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
        return YarnResourceConstants.CONTEXT_ROOT;
    }

    public String getVersion() {
        return YarnResourceConstants.API_VERSION;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URL getFullEndpointUrl() throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        sb.append(removeLeadingAndTrailingSlash(apiEndpoint));
        sb.append('/');
        sb.append(removeLeadingAndTrailingSlash(getContextRoot()));
        sb.append('/');
        sb.append(removeLeadingAndTrailingSlash(getVersion()));
        sb.append('/');
        sb.append(removeLeadingAndTrailingSlash(path));
        return new URL(sb.toString());
    }

    @VisibleForTesting
    public String removeLeadingAndTrailingSlash(String s) {
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
