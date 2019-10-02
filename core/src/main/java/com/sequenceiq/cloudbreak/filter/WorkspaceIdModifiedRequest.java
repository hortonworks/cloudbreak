package com.sequenceiq.cloudbreak.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class WorkspaceIdModifiedRequest extends HttpServletRequestWrapper {

    private static final String API_VERSION = "v4";

    private static final Pattern API_RESOURCE_PATTERN = Pattern.compile("\\/" + API_VERSION + "\\/(\\d*)\\/");

    private String replaceString;

    public WorkspaceIdModifiedRequest(HttpServletRequest request, Long workspaceId) {
        super(request);
        replaceString = "/" + API_VERSION + "/" + workspaceId + "/";
    }

    @Override
    public String getRequestURI() {
        String requestURI = super.getRequestURI();
        Matcher apiResourceMatcher = API_RESOURCE_PATTERN.matcher(requestURI);
        if (apiResourceMatcher.find()) {
            return apiResourceMatcher.replaceFirst(replaceString);
        }
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer requestURL = super.getRequestURL();
        Matcher apiResourceMatcher = API_RESOURCE_PATTERN.matcher(requestURL);
        if (apiResourceMatcher.find()) {
            return requestURL.replace(apiResourceMatcher.start(), apiResourceMatcher.end(), replaceString);
        }
        return requestURL;
    }
}
