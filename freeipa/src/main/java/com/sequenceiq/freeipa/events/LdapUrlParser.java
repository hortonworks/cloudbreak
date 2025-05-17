package com.sequenceiq.freeipa.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser;

@Component
public class LdapUrlParser extends CDPRestUrlParser {

    private static final Pattern PATTERN = Pattern.compile("v1/ldaps/?([^/]+)?/?(.*)");

    private static final int MIN_GROUP_COUNT_FOR_EVENT = 1;

    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }

    @Override
    protected String getResourceName(Matcher matcher) {
        return null;
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
        return CloudbreakEventService.LDAP_RESOURCE_TYPE;
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        if (matcher.groupCount() > MIN_GROUP_COUNT_FOR_EVENT) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    protected String getIdType(Matcher matcher) {
        return null;
    }
}
