package com.sequenceiq.cloudbreak.structuredevent;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.converter.BlueprintToBlueprintDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.ClusterToClusterDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.converter.StackToStackDetailsConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyStructuredFlowEventFactory;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
@Transactional
public class BaseLegacyStructuredFlowEventFactory implements LegacyStructuredFlowEventFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLegacyStructuredFlowEventFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterToClusterDetailsConverter clusterToClusterDetailsConverter;

    @Inject
    private NodeConfig nodeConfig;

    @Inject
    private Clock clock;

    @Inject
    private BlueprintToBlueprintDetailsConverter blueprintToBlueprintDetailsConverter;

    @Inject
    private StackToStackDetailsConverter stackToStackDetailsConverter;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    @Override
    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        String resourceType = (stack.getType() == null || stack.getType().equals(StackType.WORKLOAD))
                ? CloudbreakEventService.DATAHUB_RESOURCE_TYPE
                : CloudbreakEventService.DATALAKE_RESOURCE_TYPE;
        OperationDetails operationDetails = new OperationDetails(clock.getCurrentTimeMillis(), FLOW, resourceType, stackId, stack.getName(),
                nodeConfig.getId(), cbVersion, stack.getWorkspace().getId(), stack.getCreator().getUserId(), stack.getCreator().getUserName(),
                stack.getTenant().getName(), stack.getResourceCrn(), stack.getCreator().getUserCrn(), stack.getEnvironmentCrn(), null);
        StackDetails stackDetails = null;
        ClusterDetails clusterDetails = null;
        BlueprintDetails blueprintDetails = null;
        if (detailed) {
            stackDetails = stackToStackDetailsConverter.convert(stack);
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                clusterDetails = clusterToClusterDetailsConverter.convert(cluster);
                blueprintDetails = getIfNotNull(cluster.getBlueprint(), blueprintToBlueprintDetailsConverter::convert);
            }
        }
        StructuredFlowEvent event = new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails, blueprintDetails);
        if (exception != null) {
            event.setException(ExceptionUtils.getStackTrace(exception));
        }
        return event;
    }

    @Override
    public StructuredNotificationEvent createStructuredNotificationEvent(Long stackId, String notificationType, String message, String instanceGroupName) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        return createStructuredNotificationEvent(stack, notificationType, message, instanceGroupName);
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(Stack stack, String notificationType, String message, String instanceGroupName) {
        Long stackId = stack.getId();
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(notificationType);
        notificationDetails.setNotification(message);
        notificationDetails.setStackId(stackId);

        String stackName = stack.getName();
        String userName = stack.getCreator().getUserName();
        String userId = stack.getCreator().getUserId();

        try {
            notificationDetails.setCloud(stack.cloudPlatform());
            notificationDetails.setRegion(stack.getRegion());
            notificationDetails.setAvailabiltyZone(stack.getAvailabilityZone());
            notificationDetails.setStackName(stack.getDisplayName());
            notificationDetails.setStackStatus(stack.getStatus().name());
            notificationDetails.setNodeCount(stack.getNotDeletedInstanceMetaDataSet().size());
            Cluster cluster = stack.getCluster();
            notificationDetails.setInstanceGroup(instanceGroupName);
            if (cluster != null) {
                notificationDetails.setClusterId(cluster.getId());
                notificationDetails.setClusterName(cluster.getName());
                notificationDetails.setClusterStatus(cluster.getStatus().name());
                Blueprint blueprint = cluster.getBlueprint();
                if (blueprint != null) {
                    notificationDetails.setBlueprintId(blueprint.getId());
                    notificationDetails.setBlueprintName(blueprint.getStackName());
                }
            }
        } catch (AccessDeniedException e) {
            LOGGER.info("Access denied in structured notification event creation, user: {}, stack: {}", userName, stackId, e);
        }

        String resourceType = (stack.getType() == null || stack.getType().equals(StackType.WORKLOAD))
                ? CloudbreakEventService.DATAHUB_RESOURCE_TYPE
                : CloudbreakEventService.DATALAKE_RESOURCE_TYPE;
        OperationDetails operationDetails = new OperationDetails(clock.getCurrentTimeMillis(), NOTIFICATION, resourceType, stackId, stackName,
                nodeConfig.getInstanceUUID(), cbVersion, stack.getWorkspace().getId(), userId, userName,
                stack.getTenant().getName(), stack.getResourceCrn(), stack.getCreator().getUserCrn(), stack.getEnvironmentCrn(), null);
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }
}
