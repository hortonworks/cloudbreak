package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Set;

import jakarta.annotation.PostConstruct;

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

    private EnumMap<CloudPlatform, Set<Entitlement>> entitlementForPlatformMap;

    @PostConstruct
    public void init() {
        entitlementForPlatformMap = new EnumMap<>(CloudPlatform.class);
        entitlementForPlatformMap.put(CloudPlatform.AWS, Set.of());
        entitlementForPlatformMap.put(CloudPlatform.AZURE, Set.of(Entitlement.CDP_CB_AZURE_MULTIAZ));
        entitlementForPlatformMap.put(CloudPlatform.GCP, Set.of(Entitlement.CDP_CB_GCP_MULTIAZ));
    }

    public boolean suportedMultiAzForEnvironment(String platform) {
        return supportedMultiAzPlatforms.contains(platform);
    }

    public Set<Entitlement> getMultiAzEntitlements(CloudPlatform cloudPlatform) {
        return entitlementForPlatformMap.getOrDefault(cloudPlatform, Collections.emptySet());
    }
}
