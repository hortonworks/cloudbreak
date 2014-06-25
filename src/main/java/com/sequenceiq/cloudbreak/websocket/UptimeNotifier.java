package com.sequenceiq.cloudbreak.websocket;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.websocket.message.UptimeMessage;

@Component
public class UptimeNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(UptimeNotifier.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Scheduled(fixedDelay = 60000)
    public void sendUptime() {
        Map<Long, List<UptimeMessage>> uptimes = new HashMap<>();
        List<Cluster> clusters = (List<Cluster>) clusterRepository.findAll();
        long now = new Date().getTime();
        for (Cluster cluster : clusters) {
            Stack stack = stackRepository.findStackForCluster(cluster.getId());
            if (stack != null) {
                Long uptime = cluster.getCreationFinished() == null ? 0L : now - cluster.getCreationFinished();
                if (uptimes.containsKey(stack.getUser().getId())) {
                    uptimes.get(stack.getUser().getId()).add(new UptimeMessage(stack.getId(), uptime));
                } else {
                    uptimes.put(stack.getUser().getId(), new ArrayList<UptimeMessage>());
                    uptimes.get(stack.getUser().getId()).add(new UptimeMessage(stack.getId(), uptime));
                }
            }
        }
        LOGGER.debug("uptimes: " + uptimes);
        if (uptimes.size() > 0) {
            for (Map.Entry<Long, List<UptimeMessage>> longListEntry : uptimes.entrySet()) {
                websocketService.sendToTopicUser(longListEntry.getKey(), WebsocketEndPoint.UPTIME, longListEntry.getValue());
            }
        }
    }
}
