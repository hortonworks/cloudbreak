package com.sequenceiq.periscope.service;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Service
public class NodeDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeDeletionService.class);

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    public void deleteStoppedNodesIfPresent(Cluster cluster, String hostGroup) {
        if (Strings.isNullOrEmpty(hostGroup)) {
            LOGGER.info("Policy hostGroup is not defined for cluster: {}, skipping request to delete nodes", cluster.getStackCrn());
            return;
        }

        StackV4Response stackResponse = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        List<String> stoppedHostIds = stackResponseUtils.getStoppedCloudInstanceIdsInHostGroup(stackResponse, hostGroup);

        if (stoppedHostIds.isEmpty()) {
            LOGGER.info("No stopped nodes found on cluster: {}, skipping request to delete stopped nodes", cluster.getStackCrn());
        } else {
            deleteStoppedNodesForCluster(cluster, hostGroup, stoppedHostIds);
        }
    }

    private void deleteStoppedNodesForCluster(Cluster cluster, String hostGroup, List<String> stoppedHostIds) {
        try {
            LOGGER.info("Sending request to delete stopped nodes with hostIds: {} for cluster: {}", stoppedHostIds, cluster.getStackCrn());
            cloudbreakCommunicator.deleteInstancesForCluster(cluster, stoppedHostIds);
            createAutoscalingStoppedNodeDeletionHistoryAndNotify(cluster, hostGroup, stoppedHostIds);
        } catch (Exception e) {
            LOGGER.error("Error when trying to delete stopped nodes with hostIds: {} for cluster: {}", stoppedHostIds, cluster.getStackCrn(), e);
            createAutoscalingStoppedNodeDeletionFailedHistoryAndNotify(cluster, e, stoppedHostIds);
        }
    }

    private String getHistoryMessageForException(List<String> stoppedHostIds, Exception e) {
        String cbApiError = e.getMessage();
        if (e instanceof ClientErrorException) {
            try {
                Response exceptionResponse = ((ClientErrorException) e).getResponse();
                cbApiError = exceptionResponse.readEntity(ExceptionResponse.class).getMessage();
            } catch (Exception ex) {
                LOGGER.error("Error parsing CB API Exception: {}", e, ex);
            }
        }
        return messagesService.getMessage(MessageCode.AUTOSCALING_STOPPED_NODES_DELETION_FAILED, Lists.newArrayList(stoppedHostIds,
                cbApiError));
    }

    private void createAutoscalingStoppedNodeDeletionHistoryAndNotify(Cluster cluster, String hostGroup, List<String> stoppedHostIds) {
        ScalingStatus scalingStatus = ScalingStatus.SUCCESS;
        String statusMessage = messagesService.getMessage(MessageCode.AUTOSCALING_STOPPED_NODES_DELETION, Lists.newArrayList(stoppedHostIds, hostGroup));
        notificationSender.sendHistoryUpdateNotification(historyService.createEntry(scalingStatus, statusMessage, cluster), cluster);
    }

    private void createAutoscalingStoppedNodeDeletionFailedHistoryAndNotify(Cluster cluster, Exception ex, List<String> stoppedHostIds) {
        ScalingStatus scalingStatus = ScalingStatus.TRIGGER_FAILED;
        String statusMessage = getHistoryMessageForException(stoppedHostIds, ex);
        notificationSender.sendHistoryUpdateNotification(historyService.createEntry(scalingStatus, statusMessage, cluster), cluster);
    }
}
