package com.sequenceiq.periscope.monitor.evaluator;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.HistoryService;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTimeEvaluator.class);

    private static final String EVALUATOR_NAME = CronTimeEvaluator.class.getName();

    @Inject
    private TimeAlertRepository alertRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DateService dateService;

    @Inject
    private HistoryService historyService;

    @Inject
    private EventPublisher eventPublisher;

    private long clusterId;

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    private boolean isTrigger(TimeAlert alert) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CRON_UPDATE_RATE_IN_MILLIS);
    }

    private boolean isTrigger(TimeAlert alert, ZonedDateTime zdt) {
        return dateService.isTrigger(alert, MonitorUpdateRate.CRON_UPDATE_RATE_IN_MILLIS, zdt);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        Cluster cluster = clusterService.findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        publishIfNeeded(alertRepository.findAllByCluster(clusterId));
        LOGGER.debug("Finished cronTimeEvaluator for cluster {} in {} ms", cluster.getStackCrn(), System.currentTimeMillis() - start);
    }

    private void publishIfNeeded(List<TimeAlert> alerts) {
        TimeAlert triggeredAlert = null;
        for (TimeAlert alert : alerts) {
            boolean alertTriggerable = isTrigger(alert);
            if (isPolicyAttached(alert) && alertTriggerable && null == triggeredAlert) {
                publish(alert);
                triggeredAlert = alert;
            } else if (alertTriggerable && triggeredAlert != null) {
                historyService.createEntry(ScalingStatus.TRIGGER_FAILED, String.format(
                        "Autoscaling Schedule '%s' overlaps with '%s'.", alert.getName(), triggeredAlert.getName()),
                        alert.getCluster());
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
        eventPublisher.publishEvent(new ScalingEvent(alert));
        LOGGER.debug("Time alert '{}' triggered  for cluster '{}'", alert.getName(), alert.getCluster().getStackCrn());
    }
}
