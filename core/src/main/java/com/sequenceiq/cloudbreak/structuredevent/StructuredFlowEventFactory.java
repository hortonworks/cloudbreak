package com.sequenceiq.cloudbreak.structuredevent;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.FLOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Component
@Transactional
public class StructuredFlowEventFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private TransactionService transactionService;

    @Value("${info.app.version:}")
    private String cbVersion;

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.getByIdWithoutAuth(stackId);
        UserProfile userProfile = userProfileService.getOrCreate(stack.getAccount(), stack.getOwner());
        OperationDetails operationDetails = new OperationDetails(FLOW, "stacks", stackId, stack.getName(),
                stack.getCreator().getUserId(), stack.getCreator().getUserName(), cloudbreakNodeConfig.getId(), cbVersion,
                stack.getOrganization().getId(), stack.getAccount(), stack.getOwner(), userProfile.getUserName());
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
        return exception != null ? new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails, blueprintDetails,
                stack.getOrganization().getId(), stack.getCreator().getUserId(), ExceptionUtils.getStackTrace(exception))
                : new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails,
                blueprintDetails, stack.getOrganization().getId(), stack.getCreator().getUserId());
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(Long stackId, String notificationType, String message, String instanceGroupName) {
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(notificationType);
        notificationDetails.setNotification(message);
        notificationDetails.setStackId(stackId);

        String account = null;
        String userId = null;
        String userName = null;
        String stackName = null;

        Stack stack = stackService.getByIdWithoutAuth(stackId);
        try {
            UserProfile userProfile = userProfileService.getOrCreate(stack.getAccount(), stack.getOwner());
            account = stack.getAccount();
            userId = stack.getOwner();
            userName = userProfile.getUserName();
            stackName = stack.getName();

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
                    notificationDetails.setBlueprintName(blueprint.getAmbariName());
                }
            }
        } catch (AccessDeniedException e) {
            IdentityUser cbUser = authenticatedUserService.getCbUser();
            LOGGER.info("Access denied in structured notification event creation, user: {}, stack: {}",
                    cbUser != null ? cbUser.getUsername() : "Unknown", stackId, e);
            if (cbUser != null) {
                account = cbUser.getAccount();
                userId = cbUser.getUserId();
                userName = cbUser.getUsername();
            }
        }

        OperationDetails operationDetails = new OperationDetails(NOTIFICATION, "stacks", stackId, stackName,
                stack.getCreator().getUserId(), stack.getCreator().getUserName(), cloudbreakNodeConfig.getInstanceUUID(), cbVersion,
                stack.getOrganization().getId(), account, userId, userName);
        return new StructuredNotificationEvent(operationDetails, notificationDetails, stack.getOrganization().getId(), stack.getCreator().getUserId());
    }
}
