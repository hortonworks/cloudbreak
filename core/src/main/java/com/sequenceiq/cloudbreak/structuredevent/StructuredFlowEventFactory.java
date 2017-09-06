package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowErrorEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.service.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
@Transactional
public class StructuredFlowEventFactory {
    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Value("${info.app.version:}")
    private String cbVersion;

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.findLazy(stackId);
        OperationDetails operationDetails = new OperationDetails("FLOW", "STACK", stackId, stack.getAccount(), stack.getOwner(),
                cloudbreakNodeConfig.getInstanceUUID(), cbVersion);
        StackDetails stackDetails = null;
        ClusterDetails clusterDetails = null;
        BlueprintDetails blueprintDetails = null;
        String stackTrace = null;
        if (detailed) {
            stackDetails = conversionService.convert(stack, StackDetails.class);
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                clusterDetails = conversionService.convert(cluster, ClusterDetails.class);
                blueprintDetails = conversionService.convert(cluster.getBlueprint(), BlueprintDetails.class);
            }
        }
        return exception != null ? new StructuredFlowErrorEvent(operationDetails, flowDetails, stackDetails, clusterDetails, blueprintDetails,
                ExceptionUtils.getStackTrace(exception)) : new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails,
                blueprintDetails);
    }
}
