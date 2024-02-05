package com.sequenceiq.cloudbreak.structuredevent;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
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
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
@Transactional
public class BaseLegacyStructuredFlowEventFactory implements LegacyStructuredFlowEventFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLegacyStructuredFlowEventFactory.class);

    @Inject
    private StackDtoService stackDtoService;

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

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    @Override
    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        StackView stack = stackDtoService.getStackViewById(stackId);
        ClusterView cluster = stackDtoService.getClusterViewByStackId(stackId);
        List<InstanceGroupDto> instanceGroupDtos = stackDtoService.getInstanceMetadataByInstanceGroup(stackId);
        String resourceType = (stack.getType() == null || stack.getType().equals(StackType.WORKLOAD))
                ? CloudbreakEventService.DATAHUB_RESOURCE_TYPE
                : CloudbreakEventService.DATALAKE_RESOURCE_TYPE;
        OperationDetails operationDetails = getOperationDetails(stackId, resourceType, stack);
        StackDetails stackDetails = null;
        ClusterDetails clusterDetails = null;
        BlueprintDetails blueprintDetails = null;
        if (detailed) {
            stackDetails = stackToStackDetailsConverter.convert(stack, cluster, instanceGroupDtos);
            if (cluster != null) {
                GatewayView gateway = stackDtoService.getGatewayView(cluster.getId());
                Blueprint blueprint = stackDtoService.getBlueprint(cluster.getId());
                clusterDetails = clusterToClusterDetailsConverter.convert(cluster, stack, gateway);
                blueprintDetails = getIfNotNull(blueprint, blueprintToBlueprintDetailsConverter::convert);
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
        StackDto stackDto = stackDtoService.getById(stackId);
        return createStructuredNotificationEvent(stackDto, notificationType, message, instanceGroupName);
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(StackDtoDelegate stack, String notificationType, String message,
            String instanceGroupName) {
        Long stackId = stack.getId();
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(notificationType);
        notificationDetails.setNotification(message);
        notificationDetails.setStackId(stackId);

        String stackName = stack.getName();
        String userName = stack.getCreator().getUserName();
        String userId = stack.getCreator().getUserId();

        try {
            notificationDetails.setCloud(stack.getCloudPlatform());
            notificationDetails.setRegion(stack.getRegion());
            notificationDetails.setAvailabiltyZone(stack.getAvailabilityZone());
            notificationDetails.setStackName(stack.getDisplayName());
            notificationDetails.setStackStatus(stack.getStatus().name());
            notificationDetails.setNodeCount(stack.getNotDeletedInstanceMetaData().size());
            ClusterView cluster = stack.getCluster();
            notificationDetails.setInstanceGroup(instanceGroupName);
            if (cluster != null) {
                notificationDetails.setClusterId(cluster.getId());
                notificationDetails.setClusterName(cluster.getName());
                notificationDetails.setClusterStatus(stack.getStatus().name());
                Blueprint blueprint = stack.getBlueprint();
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
        String userCrn = Optional.ofNullable(ThreadBasedUserCrnProvider.getUserCrn())
                .orElse(stack.getCreator().getUserCrn());
        OperationDetails operationDetails = new OperationDetails(clock.getCurrentTimeMillis(), NOTIFICATION, resourceType, stackId, stackName,
                nodeConfig.getInstanceUUID(), cbVersion, stack.getWorkspace().getId(), userId, userName,
                stack.getTenant().getName(), stack.getResourceCrn(), userCrn, stack.getEnvironmentCrn(), null);
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }

    private OperationDetails getOperationDetails(Long stackId, String resourceType, StackView stack) {
        String userCrn = Optional.ofNullable(ThreadBasedUserCrnProvider.getUserCrn())
                .orElseGet(() -> regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
        return new OperationDetails(
                clock.getCurrentTimeMillis(),
                FLOW,
                resourceType,
                stackId,
                stack.getName(),
                nodeConfig.getId(),
                cbVersion,
                stack.getWorkspaceId(),
                stack.getCreator().getUserId(),
                stack.getCreator().getUserName(),
                stack.getTenantName(),
                stack.getResourceCrn(),
                userCrn,
                stack.getEnvironmentCrn(),
                null);
    }
}
