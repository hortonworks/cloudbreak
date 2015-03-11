package com.sequenceiq.periscope.monitor.evaluator;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.utils.DateUtils;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(CronTimeEvaluator.class);

    @Autowired
    private TimeAlertRepository alertRepository;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    @Override
    public void run() {
        for (TimeAlert alert : alertRepository.findAllByCluster(clusterId)) {
            String alertName = alert.getName();
            LOGGER.info(clusterId, "Checking time based alert: '{}'", alertName);
            if (isTrigger(alert, clusterId) && isPolicyAttached(alert)) {
                LOGGER.info(clusterId, "Time alert: '{}' triggers", alertName);
                publishEvent(new ScalingEvent(alert));
                break;
            }
        }
    }

    private boolean isTrigger(TimeAlert alert, long clusterId) {
        return DateUtils.isTrigger(clusterId, alert.getCron(), alert.getTimeZone(), MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }
}
