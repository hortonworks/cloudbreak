package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V1V2StackInstanceDeleteRestUrlParser extends RestUrlParser {

    public static final int RESOURCE_ID_GROUP_NUMBER = 1;

    private static final Pattern PATTERN = Pattern.compile("v[12]/stacks/(\\d+)/(.+)");

    @Override
    protected List<String> parsedMethods() {
        return List.of("DELETE");
    }

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
        return matcher.group(RESOURCE_ID_GROUP_NUMBER);
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "stacks";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return "delete/instance";
    }
}
