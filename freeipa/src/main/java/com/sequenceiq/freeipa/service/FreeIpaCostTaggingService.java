package com.sequenceiq.freeipa.service;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CDP_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.cost.CostTagging;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;

@Service
public class FreeIpaCostTaggingService implements CostTagging {

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private Clock clock;

    @Override
    public Map<String, String> prepareDefaultTags(String user, Map<String, String> sourceMap, String platform, String environmentCrn) {
        Map<String, String> result = new HashMap<>();
        result.put(defaultCostTaggingService.transform(CDP_USER_NAME.key(), platform), defaultCostTaggingService.transform(user, platform));
        result.put(defaultCostTaggingService.transform(CDP_CB_VERSION.key(), platform), defaultCostTaggingService.transform(cbVersion, platform));
        defaultCostTaggingService.addEnvironmentCrnIfPresent(result, environmentCrn, platform);
        if (sourceMap == null || Strings.isNullOrEmpty(sourceMap.get(defaultCostTaggingService.transform(OWNER.key(), platform)))) {
            result.put(defaultCostTaggingService.transform(OWNER.key(), platform), defaultCostTaggingService.transform(user, platform));
        }
        result.put(defaultCostTaggingService.transform(CDP_CREATION_TIMESTAMP.key(), platform),
                defaultCostTaggingService.transform(String.valueOf(clock.getCurrentInstant().getEpochSecond()), platform));
        return result;
    }

}
