package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V4EventRestUrlParser extends RestUrlParser {

    public static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    public static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    public static final int RESOURCE_EVENT_GROUP_NUMBER = 3;

    private static final Pattern PATTERN = Pattern.compile("v4/(\\d+)/([a-z_]+)/([a-z_]+)");

    // POST is the norm. Irregular GET requests:
    // v4/{workspaceId}/audits/zip
    // v4/{workspaceId}/clusterdefinitions/recommendation
    // v4/{workspaceId}/image_catalogs/images
    // v4/{workspaceId}/connectors/{event}
    // v4/{workspaceId}/file_systems/{event}
    @Override
    protected List<String> parsedMethods() {
        return List.of("POST", "GET");
    }

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return matcher.group(WORKSPACE_ID_GROUP_NUMBER);
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }

}
