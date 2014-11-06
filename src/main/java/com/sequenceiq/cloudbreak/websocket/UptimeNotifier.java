package com.sequenceiq.cloudbreak.websocket;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

@Component
public class UptimeNotifier {

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private NotificationSender notificationSender;

    @Scheduled(fixedDelay = 60000)
    public void sendUptime() {
        List<Cluster> clusters = (List<Cluster>) clusterRepository.findAll();
        long now = new Date().getTime();
        for (Cluster cluster : clusters) {
            Stack stack = stackRepository.findStackForCluster(cluster.getId());
            if (stack != null) {
                Long uptime = cluster.getCreationFinished() == null ? 0L : now - cluster.getCreationFinished();
                Notification notification = new Notification();
                notification.setUserName(stack.getOwner());
                notification.setStackId(stack.getId());
                notification.setEventMessage(String.valueOf(uptime));
                notificationSender.send(notification);
            }
        }
    }
}
