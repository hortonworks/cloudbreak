package com.sequenceiq.cloudbreak.websocket;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class UptimeNotifier {
    private static final String UPTIME_NOTIFICATION = "UPTIME_NOTIFICATION";

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private StackUtil stackUtil;

    @Scheduled(fixedDelay = 60000)
    public void sendUptime() {
        EnumSet<Status> statuses = EnumSet.complementOf(EnumSet.of(Status.DELETE_COMPLETED));
        List<Cluster> clusters = clusterService.findByStatuses(statuses);
        for (Cluster cluster : clusters) {
            Stack stack = stackService.getForCluster(cluster.getId());
            if (stack != null && !stack.isDeleteCompleted()) {
                Long uptime = stackUtil.getUptimeForCluster(cluster, cluster.isAvailable());
                Notification<CloudbreakEventsJson> notification = createUptimeNotification(stack, uptime);
                notificationSender.send(notification);
            }
        }
    }

    private Notification<CloudbreakEventsJson> createUptimeNotification(Stack stack, Long uptime) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setEventType(UPTIME_NOTIFICATION);
        notification.setEventMessage(String.valueOf(uptime));
        if (stack.getCredential() == null) {
            notification.setCloud("null");
        } else {
            notification.setCloud(stack.getCredential().cloudPlatform());
        }
        if (stack.getCluster() == null || stack.getCluster().getBlueprint() == null) {
            notification.setBlueprintId(null);
            notification.setBlueprintName("null");
        } else {
            notification.setBlueprintId(stack.getCluster().getBlueprint().getId());
            notification.setBlueprintName(stack.getCluster().getBlueprint().getAmbariName());
            notification.setClusterName(stack.getCluster().getName());
            notification.setClusterId(stack.getCluster().getId());
        }
        return new Notification<>(notification);
    }
}
