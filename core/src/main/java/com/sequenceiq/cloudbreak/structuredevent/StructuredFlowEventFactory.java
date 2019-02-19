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

import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDefinitionDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;
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

    @Value("${info.app.version:}")
    private String cbVersion;

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed) {
        return createStucturedFlowEvent(stackId, flowDetails, detailed, null);
    }

    public StructuredFlowEvent createStucturedFlowEvent(Long stackId, FlowDetails flowDetails, Boolean detailed, Exception exception) {
        Stack stack = stackService.getByIdWithTransaction(stackId);
        OperationDetails operationDetails = new OperationDetails(FLOW, "stacks", stackId, stack.getName(), cloudbreakNodeConfig.getId(), cbVersion,
                stack.getWorkspace().getId(), stack.getCreator().getUserId(), stack.getCreator().getUserName(), stack.getTenant().getName());
        StackDetails stackDetails = null;
        ClusterDetails clusterDetails = null;
        ClusterDefinitionDetails clusterDefinitionDetails = null;
        if (detailed) {
            stackDetails = conversionService.convert(stack, StackDetails.class);
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                clusterDetails = conversionService.convert(cluster, ClusterDetails.class);
                clusterDefinitionDetails = conversionService.convert(cluster.getClusterDefinition(), ClusterDefinitionDetails.class);
            }
        }
        return exception != null
            ? new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails, clusterDefinitionDetails,
                ExceptionUtils.getStackTrace(exception))
            : new StructuredFlowEvent(operationDetails, flowDetails, stackDetails, clusterDetails, clusterDefinitionDetails);
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(Long stackId, String notificationType, String message, String instanceGroupName) {
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(notificationType);
        notificationDetails.setNotification(message);
        notificationDetails.setStackId(stackId);

        Stack stack = stackService.getByIdWithTransaction(stackId);
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
                ClusterDefinition clusterDefinition = cluster.getClusterDefinition();
                if (clusterDefinition != null) {
                    notificationDetails.setClusterDefinitionId(clusterDefinition.getId());
                    notificationDetails.setClusterDefinitionName(clusterDefinition.getStackName());
                }
            }
        } catch (AccessDeniedException e) {
            LOGGER.info("Access denied in structured notification event creation, user: {}, stack: {}", userName, stackId, e);
        }

        OperationDetails operationDetails = new OperationDetails(NOTIFICATION, "stacks", stackId, stackName, cloudbreakNodeConfig.getInstanceUUID(),
                cbVersion, stack.getWorkspace().getId(), userId, userName, stack.getTenant().getName());
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(LdapDetails ldapDetails, String notificationType, String message,
            boolean notifyWorkspace) {
        LdapNotificationDetails notificationDetails = new LdapNotificationDetails();
        notificationDetails.setLdapDetails(ldapDetails);
        notificationDetails.setNotification(message);
        notificationDetails.setNotificationType(notificationType);

        OperationDetails operationDetails = new OperationDetails(
                NOTIFICATION,
                "ldaps",
                ldapDetails.getId(),
                ldapDetails.getName(),
                cloudbreakNodeConfig.getInstanceUUID(),
                cbVersion,
                null,
                ldapDetails.getUserId(),
                null,
                null
        );
        if (notifyWorkspace) {
            operationDetails.setWorkspaceId(ldapDetails.getWorkspaceId());
            operationDetails.setUserName(ldapDetails.getUserName());
            operationDetails.setTenant(ldapDetails.getTenantName());
        }
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }

    public StructuredNotificationEvent createStructuredNotificationEvent(RdsDetails rdsDetails, String notificationType, String message,
            boolean notifyWorkspace) {
        RdsNotificationDetails notificationDetails = new RdsNotificationDetails();
        notificationDetails.setRdsDetails(rdsDetails);
        notificationDetails.setNotification(message);
        notificationDetails.setNotificationType(notificationType);

        OperationDetails operationDetails = new OperationDetails(
                NOTIFICATION,
                "rds",
                rdsDetails.getId(),
                rdsDetails.getName(),
                cloudbreakNodeConfig.getInstanceUUID(),
                cbVersion,
                null,
                rdsDetails.getUserId(),
                null,
                null
        );
        if (notifyWorkspace) {
            operationDetails.setWorkspaceId(rdsDetails.getWorkspaceId());
            operationDetails.setUserName(rdsDetails.getUserName());
            operationDetails.setTenant(rdsDetails.getTenantName());
        }
        return new StructuredNotificationEvent(operationDetails, notificationDetails);
    }

}
