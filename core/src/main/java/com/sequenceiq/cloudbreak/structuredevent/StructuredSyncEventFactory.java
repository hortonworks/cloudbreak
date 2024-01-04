package com.sequenceiq.cloudbreak.structuredevent;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.SYNC;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.converter.BlueprintToBlueprintDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.ClusterToClusterDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.StackToStackDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
public class StructuredSyncEventFactory {

    @Inject
    private Clock clock;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @Inject
    private BlueprintToBlueprintDetailsConverter blueprintToBlueprintDetailsConverter;

    @Inject
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @Inject
    private NodeConfig nodeConfig;

    @Value("${info.app.version:}")
    private String serviceVersion;

    public StructuredSyncEvent createStructuredSyncEvent(Long resourceId) {
        StackView stack = stackDtoService.getStackViewById(resourceId);
        ClusterView cluster = stackDtoService.getClusterViewByStackId(resourceId);
        List<InstanceGroupDto> instanceGroupDtos = stackDtoService.getInstanceMetadataByInstanceGroup(resourceId);
        String resourceType = (stack.getType() == null || stack.getType().equals(StackType.WORKLOAD))
                ? CloudbreakEventService.DATAHUB_RESOURCE_TYPE
                : CloudbreakEventService.DATALAKE_RESOURCE_TYPE;
        OperationDetails operationDetails = new OperationDetails(clock.getCurrentTimeMillis(), SYNC, resourceType, resourceId, stack.getName(),
                nodeConfig.getId(), serviceVersion, stack.getWorkspaceId(), stack.getCreator().getUserId(), stack.getCreator().getUserName(),
                stack.getTenantName(), stack.getResourceCrn(), stack.getCreator().getUserCrn(), stack.getEnvironmentCrn(), null);
        StackDetails stackDetails = stackToStackDetailsConverter.convert(stack, cluster, instanceGroupDtos);
        ClusterDetails clusterDetails = null;
        BlueprintDetails blueprintDetails = null;
        if (cluster != null) {
            GatewayView gateway = stackDtoService.getGatewayView(cluster.getId());
            Blueprint blueprint = stackDtoService.getBlueprint(cluster.getId());
            clusterDetails = clusterToClusterDetailsConverter.convert(cluster, stack, gateway);
            blueprintDetails = blueprintToBlueprintDetailsConverter.convert(blueprint);
        }
        return new StructuredSyncEvent(operationDetails, stackDetails, clusterDetails, blueprintDetails);
    }
}
