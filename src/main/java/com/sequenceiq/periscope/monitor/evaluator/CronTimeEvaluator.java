package com.sequenceiq.periscope.monitor.evaluator;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.MonitorUpdateRate;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.utils.DateUtils;

@Component("CronTimeEvaluator")
@Scope("prototype")
public class CronTimeEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTimeEvaluator.class);

    @Autowired
    private TimeAlertRepository alertRepository;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    private boolean isTrigger(TimeAlert alert) {
        return DateUtils.isTrigger(alert.getCron(), alert.getTimeZone(), MonitorUpdateRate.CLUSTER_UPDATE_RATE);
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

    @Override
    public void run() {
        for (TimeAlert alert : alertRepository.findAllByCluster(clusterId)) {
            MDCBuilder.buildMdcContext(alert.getCluster());
            String alertName = alert.getName();
            LOGGER.info("Checking time based alert: '{}'", alertName);
            if (isTrigger(alert) && isPolicyAttached(alert)) {
                LOGGER.info("Time alert: '{}' triggers", alertName);
                publishEvent(new ScalingEvent(alert));
                break;
            }
        }
    }
}
