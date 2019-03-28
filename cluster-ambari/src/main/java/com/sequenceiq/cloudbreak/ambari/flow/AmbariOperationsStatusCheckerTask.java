package com.sequenceiq.cloudbreak.ambari.flow;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;

@Component
public class AmbariOperationsStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    public static final Comparable<BigDecimal> COMPLETED = new BigDecimal("100.0");

    public static final BigDecimal FAILED = BigDecimal.valueOf(-1.0);

    public static final BigDecimal PENDING = BigDecimal.ZERO;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStatusCheckerTask.class);

    private static final int MAX_RETRY = 3;

    @Inject
    private NotificationSender notificationSender;

    @Override
    public boolean checkStatus(AmbariOperations t) {
        Map<String, Integer> installRequests = t.getRequests();
        boolean allFinished = true;
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            AmbariClient ambariClient = t.getAmbariClient();
            BigDecimal installProgress = getInstallProgressFromAmbari(request, ambariClient);
            LOGGER.debug("Ambari operation: '{}', Progress: {}", request.getKey(), installProgress);
            notificationSender.send(getAmbariProgressNotification(installProgress.longValue(), t.getStack(), t.getAmbariOperationType()));
            if (FAILED.compareTo(installProgress) == 0) {
                boolean failed = true;
                for (int i = 0; i < MAX_RETRY; i++) {
                    if (getInstallProgressFromAmbari(request, ambariClient).compareTo(FAILED) != 0) {
                        failed = false;
                        break;
                    }
                }
                if (failed) {
                    notificationSender.send(getAmbariProgressNotification(Long.parseLong("100"), t.getStack(), t.getAmbariOperationType()));
                    throw new AmbariOperationFailedException(String.format("Ambari operation failed: [component: '%s', requestID: '%s']", request.getKey(),
                            request.getValue()));
                }
            }
            allFinished = allFinished && COMPLETED.compareTo(installProgress) == 0;
        }
        return allFinished;
    }

    private BigDecimal getInstallProgressFromAmbari(Entry<String, Integer> request, AmbariClient ambariClient) {
        try {
            return Optional.ofNullable(ambariClient.getRequestProgress(request.getValue())).orElse(PENDING);
        } catch (AmbariConnectionException e) {
            LOGGER.warn(e.getMessage());
            return FAILED;
        } catch (Exception e) {
            LOGGER.info("Failed to retrieve Ambari request progress.", e);
            return FAILED;
        }
    }

    private Notification<CloudbreakEventV4Response> getAmbariProgressNotification(Long progressValue, Stack stack, AmbariOperationType ambariOperationType) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventType(ambariOperationType.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(String.valueOf(progressValue));
        notification.setUserId(stack.getCreator().getUserId());
        notification.setTenantName(stack.getCreator().getTenant().getName());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setCloud(stack.cloudPlatform());
        notification.setRegion(stack.getRegion());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        if (stack.getCluster() != null) {
            notification.setClusterId(stack.getCluster().getId());
            notification.setClusterName(stack.getCluster().getName());
            notification.setClusterStatus(stack.getCluster().getStatus());
        }
        return new Notification<>(notification);
    }

    @Override
    public void handleTimeout(AmbariOperations t) {
        throw new IllegalStateException(String.format("Ambari operations timed out: %s", t.getRequests()));
    }

    @Override
    public String successMessage(AmbariOperations t) {
        return String.format("Requested Ambari operations completed: %s", t.getRequests().toString());
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.info("Ambari operation failed.", e);
    }

}
