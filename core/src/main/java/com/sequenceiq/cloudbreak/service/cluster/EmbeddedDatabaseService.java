package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class EmbeddedDatabaseService {

    @Inject
    private CloudParameterCache cloudParameterCache;

    public boolean isEmbeddedDatabaseOnAttachedDiskEnabled(StackDtoDelegate stack, ClusterView cluster) {
        DatabaseAvailabilityType externalDatabase = ObjectUtils.defaultIfNull(stack.getExternalDatabaseCreationType(), DatabaseAvailabilityType.NONE);
        String databaseCrn = cluster == null ? "" : cluster.getDatabaseServerCrn();
        return DatabaseAvailabilityType.NONE == externalDatabase
                && StringUtils.isEmpty(databaseCrn)
                && cloudParameterCache.isVolumeAttachmentSupported(stack.getCloudPlatform());
    }

    public boolean isAttachedDiskForEmbeddedDatabaseCreated(StackDto stack) {
        Optional<InstanceGroupView> gatewayGroup = stack.getGatewayGroup();
        return stack.getCluster().getEmbeddedDatabaseOnAttachedDisk()
                && calculateVolumeCountOnGatewayGroup(gatewayGroup.map(InstanceGroupView::getTemplate)) > 0;
    }

    public boolean isAttachedDiskForEmbeddedDatabaseCreated(ClusterView cluster, Optional<InstanceGroupView> gatewayGroup) {
        return cluster.getEmbeddedDatabaseOnAttachedDisk() && calculateVolumeCountOnGatewayGroup(gatewayGroup.map(InstanceGroupView::getTemplate)) > 0;
    }

    private int calculateVolumeCountOnGatewayGroup(Optional<Template> gatewayGroupTemplate) {
        Template template = gatewayGroupTemplate.orElse(null);
        return template == null ? 0 : template.getVolumeTemplates().stream()
                .filter(volumeTemplate -> volumeTemplate.getUsageType() == VolumeUsageType.DATABASE)
                .mapToInt(VolumeTemplate::getVolumeCount).sum();
    }
}
