package com.sequenceiq.periscope.monitor.evaluator;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTimeEvaluator.class);

    @Inject
    private TimeAlertRepository alertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DateService dateService;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    private boolean isTrigger(TimeAlert alert) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    private boolean isTrigger(TimeAlert alert, ZonedDateTime zdt) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CLUSTER_UPDATE_RATE, zdt);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

    @Override
    public void run() {
        Cluster cluster = clusterService.findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        publishIfNeeded(alertRepository.findAllByCluster(clusterId));
    }

    public void publishIfNeeded(List<TimeAlert> alerts) {
        for (TimeAlert alert : alerts) {
            if (isPolicyAttached(alert) && isTrigger(alert)) {
                publish(alert);
                break;
            }
        }
    }

    public void publishIfNeeded(Map<TimeAlert, ZonedDateTime> alerts) {
        for (Entry<TimeAlert, ZonedDateTime> alertEntry : alerts.entrySet()) {
            TimeAlert alert = alertEntry.getKey();
            if (isPolicyAttached(alert) && isTrigger(alert, alertEntry.getValue())) {
                publish(alert);
                break;
            }
        }
    }

    private void publish(TimeAlert alert) {
        LOGGER.info("Time alert '{}' triggers the '{}' scaling policy", alert.getName(), alert.getScalingPolicy().getName());
        publishEvent(new ScalingEvent(alert));
    }
}
