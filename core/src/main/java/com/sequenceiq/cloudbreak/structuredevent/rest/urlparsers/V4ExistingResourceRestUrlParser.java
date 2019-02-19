package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V4ExistingResourceRestUrlParser extends RestUrlParser {

    public static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    public static final int RESOURCE_NAME_GROUP_NUMBER = 3;

    public static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    // Irregular requests with resource ID instead of resource name: v4/{workspaceId}/audits/{auditId}
    // Irregular GET requests with event but no resource name: v4/{workspaceId}/audits/zip and remaining patterns
    private static final Pattern ANTI_PATTERN = Pattern.compile("v4/\\d+/(audits/.*|clusterdefinitions/recommendation|image_catalogs/images"
            + "|connectors/[a-z_]+|file_systems/[a-z_]+)");

    private static final Pattern PATTERN = Pattern.compile("v4/(\\d+)/([a-z_]+)/([^/]+)");

    @Override
    protected List<String> parsedMethods() {
        return List.of("DELETE", "PUT", "GET");
    }

    @Override
    protected Pattern getAntiPattern() {
        return ANTI_PATTERN;
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
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return matcher.group(RESOURCE_TYPE_GROUP_NUMBER);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }

}
