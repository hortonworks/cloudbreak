package com.sequenceiq.cloudbreak.service.cluster.flow;

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
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

@Component
public class AmbariOperationsStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    public static final Comparable<BigDecimal> COMPLETED = new BigDecimal(100.0);

    public static final BigDecimal FAILED = new BigDecimal(-1.0);

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
            BigDecimal installProgress = Optional.ofNullable(ambariClient.getRequestProgress(request.getValue())).orElse(PENDING);
            LOGGER.info("Ambari operation: '{}', Progress: {}", request.getKey(), installProgress);
            notificationSender.send(getAmbariProgressNotification(installProgress.longValue(), t.getStack(), t.getAmbariOperationType()));
            if (FAILED.compareTo(installProgress) == 0) {
                boolean failed = true;
                for (int i = 0; i < MAX_RETRY; i++) {
                    if (ambariClient.getRequestProgress(request.getValue()).compareTo(FAILED) != 0) {
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

    private Notification<CloudbreakEventsJson> getAmbariProgressNotification(Long progressValue, Stack stack, AmbariOperationType ambariOperationType) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType(ambariOperationType.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(String.valueOf(progressValue));
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setCloud(stack.cloudPlatform());
        notification.setRegion(stack.getRegion());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        notification.setStackName(stack.getName());
        if (stack.getCluster() != null) {
            notification.setClusterId(stack.getCluster().getId());
            notification.setClusterName(stack.getCluster().getName());
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
