package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class V4ExistingResourceEventRestUrlParser extends LegacyRestUrlParser {

    private static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    private static final int RESOURCE_NAME_GROUP_NUMBER = 3;

    private static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    private static final int RESOURCE_EVENT_GROUP_NUMBER = 4;

    // Irregular requests containing event followed by resource name at the end: v4/{workspaceId}/credentials/*
    // Irregular requests with resource name format followed by resource name at the end: remaining patterns
    private static final Pattern ANTI_PATTERN = Pattern.compile("v4/\\d+/(credentials/.+|(blueprints)|(cluster_templates)/(name|crn)/([^/]+))");

    private static final Pattern PATTERN = Pattern.compile("v4/(\\d+)/([a-z_]+)/([^/]+)/([a-z_]+)");

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
    protected String getResourceCrn(Matcher matcher) {
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
