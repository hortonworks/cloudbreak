package com.sequenceiq.freeipa.service;

import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.DISK;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.INSTANCE;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.IP;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.NETWORK;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.SECURITY;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.STORAGE;
import static com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType.TEMPLATE;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag;
import com.sequenceiq.cloudbreak.service.Clock;

@Service
public class CostTaggingService {

    @Value("${info.app.version:}")
    private String version;

    @Inject
    private Clock clock;

    public Map<String, String> prepareInstanceTagging() {
        return prepareResourceTag(INSTANCE);
    }

    public Map<String, String> prepareNetworkTagging() {
        return prepareResourceTag(NETWORK);
    }

    public Map<String, String> prepareTemplateTagging() {
        return prepareResourceTag(TEMPLATE);
    }

    public Map<String, String> prepareSecurityTagging() {
        return prepareResourceTag(SECURITY);
    }

    public Map<String, String> prepareIpTagging() {
        return prepareResourceTag(IP);
    }

    public Map<String, String> prepareDiskTagging() {
        return prepareResourceTag(DISK);
    }

    public Map<String, String> prepareStorageTagging() {
        return prepareResourceTag(STORAGE);
    }

    public Map<String, String> prepareAllTagsForTemplate() {
        Map<String, String> result = new HashMap<>();
        for (CloudbreakResourceType cloudbreakResourceType : CloudbreakResourceType.values()) {
            result.put(cloudbreakResourceType.templateVariable(), cloudbreakResourceType.key());
        }
        return result;
    }

    public Map<String, String> prepareDefaultTags(String user, Map<String, String> sourceMap, String platform) {
        Map<String, String> result = new HashMap<>();
        result.put(transform(CB_USER_NAME.key(), platform), transform(user, platform));
        result.put(transform(CB_VERSION.key(), platform), transform(version, platform));
        if (sourceMap == null || Strings.isNullOrEmpty(sourceMap.get(transform(OWNER.key(), platform)))) {
            result.put(transform(OWNER.key(), platform), transform(user, platform));
        }
        result.put(transform(CB_CREATION_TIMESTAMP.key(), platform), transform(String.valueOf(clock.getCurrentInstant().getEpochSecond()), platform));
        return result;
    }

    private String transform(String value, String platform) {
        String valueAfterCheck = Strings.isNullOrEmpty(value) ? "unknown" : value;
        return CloudConstants.GCP.equals(platform)
                ? valueAfterCheck.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-") : valueAfterCheck;
    }

    private Map<String, String> prepareResourceTag(CloudbreakResourceType cloudbreakResourceType) {
        Map<String, String> result = new HashMap<>();
        result.put(DefaultApplicationTag.CB_RESOURCE_TYPE.key(), cloudbreakResourceType.key());
        return result;
    }
}
