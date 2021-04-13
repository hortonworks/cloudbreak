package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.LegacyRestUrlParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class V4ImageCatalogGetImageByNameAndByIdRestUrlParser extends LegacyRestUrlParser {

    private static final int WORKSPACE_ID_GROUP_NUMBER = 1;

    private static final int RESOURCE_NAME_GROUP_NUMBER = 3;

    private static final int RESOURCE_TYPE_GROUP_NUMBER = 2;

    // v4/{workspaceId}/image_catalogs/{name}/image/{imageId}
    private static final Pattern PATTERN = Pattern.compile("v4/(\\d+)/(image_catalogs)\\/([\\w\\_\\:\\s]+)\\/image\\/([\\w\\_\\:\\s]+)?");

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