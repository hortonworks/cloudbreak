package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowErrorEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Component
@Transactional(readOnly = true)
public class StructuredFlowEventFactory {

    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private UserProfileService userProfileService;

    @Value("${info.app.version:}")
    private String cbVersion;

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.getById(stackId);
        UserProfile userProfile = userProfileService.get(stack.getAccount(), stack.getOwner());
        OperationDetails operationDetails = new OperationDetails("FLOW", "STACK", stackId, stack.getAccount(), stack.getOwner(),
                userProfile.getUserName(), cloudbreakNodeConfig.getId(), cbVersion);
        StackDetails stackDetails = null;
        ClusterDetails clusterDetails = null;
        BlueprintDetails blueprintDetails = null;
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

    public StructuredNotificationEvent createStructuredNotificationEvent(Long stackId, String notificationType, String message, String instanceGroupName) {
        Stack stack = stackService.getById(stackId);
        UserProfile userProfile = userProfileService.get(stack.getAccount(), stack.getOwner());
        OperationDetails operationDetails = new OperationDetails("NOTIFICATION", "STACK", stackId, stack.getAccount(), stack.getOwner(),
                userProfile.getUserName(), cloudbreakNodeConfig.getInstanceUUID(), cbVersion);
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(notificationType);
        notificationDetails.setNotification(message);
        notificationDetails.setCloud(stack.cloudPlatform());
        notificationDetails.setRegion(stack.getRegion());
        notificationDetails.setAvailabiltyZone(stack.getAvailabilityZone());
        notificationDetails.setStackId(stackId);
        notificationDetails.setStackName(stack.getDisplayName());
        notificationDetails.setStackStatus(stack.getStatus().name());
        notificationDetails.setNodeCount(stack.getNotDeletedInstanceMetaDataSet().size());
        notificationDetails.setInstanceGroup(instanceGroupName);
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            notificationDetails.setClusterId(cluster.getId());
            notificationDetails.setClusterName(cluster.getName());
            notificationDetails.setClusterStatus(cluster.getStatus().name());
            Blueprint blueprint = cluster.getBlueprint();
            if (blueprint != null) {
                notificationDetails.setBlueprintId(blueprint.getId());
                notificationDetails.setBlueprintName(blueprint.getAmbariName());
            }
        }
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }
}
