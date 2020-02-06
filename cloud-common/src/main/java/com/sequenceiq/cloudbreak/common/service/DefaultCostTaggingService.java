package com.sequenceiq.cloudbreak.common.service;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CREATOR_CRN;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.ENVIRONMENT_CRN;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.owner;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.cost.CostTagging;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag;

@Service
public class DefaultCostTaggingService implements CostTagging {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCostTaggingService.class);

    @Inject
    private Clock clock;

    @Override
    public Map<String, String> prepareDefaultTags(CDPTagGenerationRequest request) {
        LOGGER.debug("About to prepare default tag(s)...");
        Map<String, String> result = new HashMap<>();
        String platform = request.getPlatform();
        addCDPCrnIfPresent(result, ENVIRONMENT_CRN, request.getEnvironmentCrn(), platform);
        addCDPCrnIfPresent(result, CREATOR_CRN, request.getCreatorCrn(), platform);
        addCDPCrnIfPresent(result, RESOURCE_CRN, request.getResourceCrn(), platform);

        // Internal tags should be based on entitlement
        addTagIfNotPresented(result, request, OWNER, request.getUserName());
        addTagIfNotPresented(result, request, owner, request.getUserName());
        addTagIfNotPresented(result, request, CREATION_TIMESTAMP, String.valueOf(clock.getCurrentInstant().getEpochSecond()));
        LOGGER.debug("The following default tag(s) has prepared: {}", result);
        return result;
    }

    private void addTagIfNotPresented(Map<String, String> result, CDPTagGenerationRequest request, DefaultApplicationTag key, String value) {
        if (request.isKeyNotPresented(key)) {
            result.put(transform(key.key(), request.getPlatform()), transform(value, request.getPlatform()));
        }
    }

    private void addCDPCrnIfPresent(Map<String, String> result, DefaultApplicationTag tag, String crn, String platform) {
        if (StringUtils.isNotEmpty(crn)) {
            result.put(transform(tag.key(), platform), crn);
        } else {
            LOGGER.debug("Unable to add \"{}\" - cost - tag to the resource's default tags because it's value is empty or null!", tag.key());
        }
    }

    private String transform(String value, String platform) {
        String valueAfterCheck = Strings.isNullOrEmpty(value) ? "unknown" : value;
        return CloudConstants.GCP.equals(platform)
                ? valueAfterCheck.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-") : valueAfterCheck;
    }
}
