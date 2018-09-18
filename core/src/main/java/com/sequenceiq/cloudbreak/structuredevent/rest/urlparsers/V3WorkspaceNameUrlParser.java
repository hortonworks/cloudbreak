package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V3WorkspaceNameUrlParser extends RestUrlParser {

    public static final int RESOURCE_NAME_GROUP_NUMBER = 1;

    // /v3/workspaces/name/abcdabcd/addUsers
    private static final Pattern PATTERN = Pattern.compile("v3/workspaces/name/([^\\d][^/]+)");

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getWorkspaceId(Matcher matcher) {
        return null;
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
        return "workspaces";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }
}
