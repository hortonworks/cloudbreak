package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;

public abstract class RestUrlParser {

    public static final String RESOURCE_TYPE = "RESOURCE_TYPE";

    public static final String RESOURCE_ID = "RESOURCE_ID";

    public static final String RESOURCE_NAME = "RESOURCE_NAME";

    public static final String RESOURCE_EVENT = "RESOURCE_EVENT";

    public static final String WORKSPACE_ID = "WORKSPACE";

    public String getUrl(ContainerRequestContext requestContext) {
        return requestContext.getUriInfo().getPath();
    }

    private String getMethod(ContainerRequestContext requestContext) {
        return requestContext.getMethod();
    }

    public boolean fillParams(ContainerRequestContext requestContext, Map<String, String> params) {
        if (parsedMethods().stream().anyMatch(method -> method.equals(getMethod(requestContext)))) {
            return fillParams(getUrl(requestContext), params);
        } else {
            return false;
        }
    }

    private boolean fillParams(String url, Map<String, String> params) {
        if (checkAntiPattern(url)) {
            return false;
        }

        Pattern pattern = getPattern();
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            params.put(WORKSPACE_ID, getWorkspaceId(matcher));
            params.put(RESOURCE_NAME, getResourceName(matcher));
            params.put(RESOURCE_ID, getResourceId(matcher));
            params.put(RESOURCE_TYPE, getResourceType(matcher));
            params.put(RESOURCE_EVENT, getResourceEvent(matcher));
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAntiPattern(String url) {
        Pattern antiPattern = getAntiPattern();
        if (antiPattern != null) {
            Matcher antiMatcher = antiPattern.matcher(url);
            return antiMatcher.matches();
        }
        return false;
    }

    protected List<String> parsedMethods() {
        return List.of("DELETE", "POST", "PUT", "GET");
    }

    protected abstract Pattern getPattern();

    protected Pattern getAntiPattern() {
        return null;
    }

    protected abstract String getWorkspaceId(Matcher matcher);

    protected abstract String getResourceName(Matcher matcher);

    protected abstract String getResourceId(Matcher matcher);

    protected abstract String getResourceType(Matcher matcher);

    protected abstract String getResourceEvent(Matcher matcher);

}
