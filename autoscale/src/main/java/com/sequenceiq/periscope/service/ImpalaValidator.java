package com.sequenceiq.periscope.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class ImpalaValidator {

    private Map<CloudPlatform, Set<Entitlement>> entitlementForPlatformMap = new HashMap<>() {
        {
            put(CloudPlatform.AWS, Set.of(Entitlement.DATAHUB_AWS_AUTOSCALING, Entitlement.DATAHUB_AWS_IMPALA_SCHEDULE_BASED_SCALING));
            put(CloudPlatform.AZURE, Set.of(Entitlement.DATAHUB_AZURE_AUTOSCALING, Entitlement.DATAHUB_AZURE_IMPALA_SCHEDULE_BASED_SCALING));
            put(CloudPlatform.GCP, Set.of(Entitlement.DATAHUB_GCP_AUTOSCALING, Entitlement.DATAHUB_GCP_IMPALA_SCHEDULE_BASED_SCALING));
        }
    };

    public Set<Entitlement> getImpalaScheduleScalingEntitlements(CloudPlatform cloudPlatform) {
        return entitlementForPlatformMap.getOrDefault(cloudPlatform, Collections.emptySet());
    }
}
