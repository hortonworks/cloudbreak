package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class V4ExistingResourceByCrnOrNameRestUrlParser extends LegacyRestUrlParser {

    private static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    private static final int RESOURCE_NAME_GROUP_NUMBER = 3;

    private static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    // v4/{workspaceId}/blueprints/name/{name}
    // v4/{workspaceId}/blueprints/crn/{name}
    private static final Pattern PATTERN = Pattern
            .compile("v4/(\\d+)/(blueprints|image_catalogs|recipes|stacks|distrox|cluster_templates)/(?:name|crn)/([^/]+)(/\\w+)?");

    @Override
    protected List<String> parsedMethods() {
        return List.of("DELETE", "GET", "PUT");
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
        return matcher.group(RESOURCE_NAME_GROUP_NUMBER);
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        return matcher.group(RESOURCE_NAME_GROUP_NUMBER);
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
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

}
