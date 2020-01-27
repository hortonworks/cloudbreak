package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class FlowRestUrlParser extends RestUrlParser {

    public static final int RESOURCE_CRN_GROUP_NUMBER = 5;

    public static final int RESOURCE_NAME_GROUP_NUMBER = 4;

    public static final int FLOW_ID_GROUP_NUMBER = 8;

    public static final int CHECK_FLOW_ID_GROUP_NUMBER = 6;

    public static final int CHECK_CHAIN_ID_GROUP_NUMBER = 7;

    private static final Pattern PATTERN = Pattern.compile("flow_logs/((resource/|check/)(name/([^/]+)|" +
            "crn/([^/]+)|flowId/([^/]+)|chainId/([^/]+))|([^/]+))(/last)?");

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
        return getResourceNameRecursively(matcher, Lists.newArrayList(RESOURCE_NAME_GROUP_NUMBER, FLOW_ID_GROUP_NUMBER,
                RESOURCE_CRN_GROUP_NUMBER, CHECK_FLOW_ID_GROUP_NUMBER, CHECK_CHAIN_ID_GROUP_NUMBER).iterator());
    }

    @Override
    protected String getResourceCrn(Matcher matcher) {
        return StringUtils.isNotBlank(matcher.group(RESOURCE_CRN_GROUP_NUMBER)) ? matcher.group(RESOURCE_CRN_GROUP_NUMBER) : null;
    }

    @Override
    protected String getResourceId(Matcher matcher) {
        return null;
    }

    @Override
    protected String getResourceType(Matcher matcher) {
        return "flow_logs";
    }

    @Override
    protected String getResourceEvent(Matcher matcher) {
        return null;
    }

    private String getResourceNameRecursively(Matcher matcher, Iterator<Integer> groupNumbersIterator) {
        if (groupNumbersIterator.hasNext()) {
            Integer next = groupNumbersIterator.next();
            return StringUtils.isNotBlank(matcher.group(next)) ? matcher.group(next) : getResourceNameRecursively(matcher, groupNumbersIterator);
        }
        return null;
    }

}
