package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V1RestUrlParser extends RestUrlParser {

    public static final int RESOURCE_TYPE_GROUP_NUMBER = 1;

    private static final Pattern PATTERN = Pattern.compile("v1/([a-z|-]+)");

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
        return null;
    }
}
