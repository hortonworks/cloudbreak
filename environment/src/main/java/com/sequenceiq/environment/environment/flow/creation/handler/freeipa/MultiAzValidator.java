package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class MultiAzValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzValidator.class);

    @Value("${cb.multiaz.supported.platform:AWS,AZURE,GCP}")
    private Set<String> supportedMultiAzPlatforms;

    private Map<CloudPlatform, Set<Entitlement>> entitlementForPlatformMap = new HashMap<>() {
        {
            put(CloudPlatform.AWS, Set.of());
            put(CloudPlatform.AZURE, Set.of(Entitlement.CDP_CB_AZURE_MULTIAZ));
            put(CloudPlatform.GCP, Set.of(Entitlement.CDP_CB_GCP_MULTIAZ));
        }
    };

    public boolean suportedMultiAzForEnvironment(String platform) {
        return supportedMultiAzPlatforms.contains(platform);
    }

    public Set<Entitlement> getMultiAzEntitlements(CloudPlatform cloudPlatform) {
        return entitlementForPlatformMap.getOrDefault(cloudPlatform, Collections.emptySet());
    }
}
