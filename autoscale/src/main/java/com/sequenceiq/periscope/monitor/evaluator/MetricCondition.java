package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

@Component
public class MetricCondition {

    public static final String ALERT_TS = "original_timestamp";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricCondition.class);

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

    public boolean isMetricAlertTriggered(AmbariClient ambariClient, MetricAlert metricAlert) {
        String alertName = metricAlert.getName();
        LOGGER.info("Checking metric based alert: '{}'", alertName);
        List<Map<String, Object>> alerts = ambariRequestLogging.logging(() ->
                ambariClient.getAlertByNameAndState(metricAlert.getDefinitionName(), metricAlert.getAlertState().getValue()), "alert");
        if (!alerts.isEmpty()) {
            long elapsedTime = alerts.stream().map(this::getPeriod).max(Long::compareTo).get();
            LOGGER.info("Alert: {} is in '{}' state since {} min(s). Alerts size: [{}]", alertName, metricAlert.getAlertState().getValue(),
                    ClusterUtils.TIME_FORMAT.format((double) elapsedTime / TimeUtil.MIN_IN_MS), alerts.size());
            return isPeriodReached(metricAlert, elapsedTime);
        } else {
            LOGGER.info("No alert with name: [{}] in state [{}]", metricAlert.getDefinitionName(), metricAlert.getAlertState().getValue());
            return false;
        }
    }

    private long getPeriod(Map<String, Object> alert) {
        return System.currentTimeMillis() - (long) alert.get(ALERT_TS);
    }

    private boolean isPeriodReached(MetricAlert alert, float period) {
        return period > alert.getPeriod() * TimeUtil.MIN_IN_MS;
    }

}
