package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class EmbeddedDatabaseService {
    @Inject
    private EntitlementService entitlementService;

    public EmbeddedDatabaseInfo getEmbeddedDatabaseInfo(String actorCrn, String accountId, Stack stack, Cluster cluster) {
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        String databaseCrn = cluster == null ? "" : cluster.getDatabaseServerCrn();
        if (DatabaseAvailabilityType.NONE == externalDatabase && StringUtils.isEmpty(databaseCrn)
                && entitlementService.embeddedDatabaseOnAttachedDiskEnabled(accountId)) {
            int volumeCount = calculateVolumeCountOnGatewayGroup(stack);
            return new EmbeddedDatabaseInfo(volumeCount);
        } else {
            return new EmbeddedDatabaseInfo(0);
        }
    }

    public EmbeddedDatabaseInfo getEmbeddedDatabaseInfo(Stack stack) {
        if (stack.getCluster().getEmbeddedDatabaseOnAttachedDisk()) {
            int volumeCount = calculateVolumeCountOnGatewayGroup(stack);
            return new EmbeddedDatabaseInfo(volumeCount);
        } else {
            return new EmbeddedDatabaseInfo(0);
        }
    }

    private int calculateVolumeCountOnGatewayGroup(Stack stack) {
        Optional<InstanceGroup> gatewayGroup = stack.getInstanceGroups().stream()
                .filter(ig -> ig.getInstanceGroupType() == InstanceGroupType.GATEWAY).findFirst();
        Template template = gatewayGroup.map(InstanceGroup::getTemplate).orElse(null);
        return template == null ? 0 : template.getVolumeTemplates().stream()
                .mapToInt(volume -> volume.getVolumeCount()).sum();
    }
}
