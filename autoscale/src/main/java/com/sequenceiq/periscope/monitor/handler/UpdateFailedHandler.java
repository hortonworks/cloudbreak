package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_SUSPENDED;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final int RETRY_THRESHOLD = 15;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HistoryService historyService;

    @Inject
    private CloudbreakMessagesService messagesService;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        MDCBuilder.buildMdcContext(cluster);
        Integer failed = Optional.ofNullable(updateFailures.get(autoscaleClusterId))
                .map(failedCount -> failedCount + 1)
                .orElse(1);
        if (failed < RETRY_THRESHOLD) {
            updateFailures.put(autoscaleClusterId, failed);
            LOGGER.info("Increased Autoscaling failure count '{}' for cluster '{}'", failed, cluster.getStackCrn());
        } else {
            clusterService.setAutoscaleState(cluster.getId(), false);
            updateFailures.remove(autoscaleClusterId);
            historyService.createEntry(ScalingStatus.DISABLED, messagesService.getMessage(AUTOSCALING_SUSPENDED), cluster);
            LOGGER.info("Suspended Autoscaling for cluster '{}' due to repeated failure of scaling attempts", cluster.getStackCrn());
        }
    }
}