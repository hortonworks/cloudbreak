package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class V1SetDefaultByIdRestUrlParser extends RestUrlParser {

    private static final Pattern PATTERN = Pattern.compile("v1/([a-z|-]*)/setdefault/(\\d+)");

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
        return matcher.group(2);
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return matcher.group(1);
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return "setdefault";
    }
}
