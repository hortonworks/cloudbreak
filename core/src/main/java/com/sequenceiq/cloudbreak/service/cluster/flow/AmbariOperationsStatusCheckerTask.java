package com.sequenceiq.cloudbreak.service.cluster.flow;

import com.google.common.base.Optional;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

@Component
public class AmbariOperationsStatusCheckerTask extends ClusterBasedStatusCheckerTask<AmbariOperations> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationsStatusCheckerTask.class);

    private static final BigDecimal COMPLETED = new BigDecimal(100.0);
    private static final BigDecimal FAILED = new BigDecimal(-1.0);
    private static final BigDecimal PENDING = new BigDecimal(0);
    private static final int MAX_RETRY = 3;

    @Inject
    private NotificationSender notificationSender;

    @Override
    public boolean checkStatus(AmbariOperations t) {
        Map<String, Integer> installRequests = t.getRequests();
        boolean allFinished = true;
        for (Entry<String, Integer> request : installRequests.entrySet()) {
            AmbariClient ambariClient = t.getAmbariClient();
            BigDecimal installProgress = Optional.fromNullable(ambariClient.getRequestProgress(request.getValue())).or(PENDING);
            LOGGER.info("Ambari operation: '{}', Progress: {}", request.getKey(), installProgress);
            notificationSender.send(getAmbariProgressNotification(installProgress.longValue(), t.getStack(), t.getAmbariOperationType()));
            allFinished = allFinished && COMPLETED.compareTo(installProgress) == 0;
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
        }
        return allFinished;
    }

    private Notification getAmbariProgressNotification(Long progressValue, Stack stack, AmbariOperationType ambariOperationType) {
        Notification notification = new Notification();
        notification.setEventType(ambariOperationType.name());
        notification.setEventTimestamp(new Date());
        notification.setEventMessage(String.valueOf(progressValue));
        notification.setOwner(stack.getOwner());
        notification.setAccount(stack.getAccount());
        notification.setCloud(stack.cloudPlatform().toString());
        notification.setRegion(stack.getRegion());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        return notification;
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
        LOGGER.error("Ambari operation failed.", e);
    }

}
