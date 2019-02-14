package com.sequenceiq.cloudbreak.websocket;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
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
                Notification<CloudbreakEventV4Response> notification = createUptimeNotification(stack, uptime);
                notificationSender.send(notification);
            }
        }
    }

    private Notification<CloudbreakEventV4Response> createUptimeNotification(Stack stack, Long uptime) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        notification.setEventType(UPTIME_NOTIFICATION);
        notification.setEventMessage(String.valueOf(uptime));
        if (stack.getCredential() == null) {
            notification.setCloud("null");
        } else {
            notification.setCloud(stack.getCredential().cloudPlatform());
        }
        if (stack.getCluster() == null || stack.getCluster().getClusterDefinition() == null) {
            notification.setBlueprintId(null);
            notification.setBlueprintName("null");
        } else {
            notification.setBlueprintId(stack.getCluster().getClusterDefinition().getId());
            notification.setBlueprintName(stack.getCluster().getClusterDefinition().getStackName());
            notification.setClusterName(stack.getCluster().getName());
            notification.setClusterId(stack.getCluster().getId());
            notification.setClusterStatus(stack.getCluster().getStatus());
        }
        return new Notification<>(notification);
    }
}
