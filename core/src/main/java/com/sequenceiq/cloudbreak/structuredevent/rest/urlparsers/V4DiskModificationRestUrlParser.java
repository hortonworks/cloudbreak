package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;

@Component
public class V4DiskModificationRestUrlParser extends LegacyRestUrlParser {

    private static final int RESOURCE_NAME_GROUP_NUMBER = 1;

    private static final int RESOURCE_EVENT_GROUP_NUMBER = 2;

    private static final Pattern PATTERN = Pattern.compile("v4/disks/([\\w\\_\\:\\s]+)/([A-Za-z_]+)(/(.*))?");

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
    protected String getResourceCrn(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "disks";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return matcher.group(RESOURCE_EVENT_GROUP_NUMBER);
    }

}
