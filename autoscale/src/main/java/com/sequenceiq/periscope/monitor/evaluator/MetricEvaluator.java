package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.AmbariClientProvider;
import com.sequenceiq.periscope.utils.ClusterUtils;

//@Component("MetricEvaluator")
//@Scope("prototype")
public class MetricEvaluator extends AbstractEventPublisher implements EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricEvaluator.class);

    private static final String ALERT_STATE = "state";

    private static final String ALERT_TS = "timestamp";

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private MetricAlertRepository alertRepository;

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    private long clusterId;

    @Override
    public void setContext(Map<String, Object> context) {
        this.clusterId = (long) context.get(EvaluatorContext.CLUSTER_ID.name());
    }

    @Override
    public void run() {
        Cluster cluster = clusterService.find(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
        try {
            for (MetricAlert alert : alertRepository.findAllByCluster(clusterId)) {
                String alertName = alert.getName();
                LOGGER.info("Checking metric based alert: '{}'", alertName);
                List<Map<String, Object>> alertHistory = ambariClient.getAlertHistory(alert.getDefinitionName(), 1);
                int historySize = alertHistory.size();
                if (historySize > 1) {
                    LOGGER.debug("Multiple results found for alert: {}, probably HOST alert, ignoring now..", alertName);
                    continue;
                }
                if (!alertHistory.isEmpty()) {
                    Map<String, Object> history = alertHistory.get(0);
                    String currentState = (String) history.get(ALERT_STATE);
                    if (isAlertStateMet(currentState, alert)) {
                        long elapsedTime = getPeriod(history);
                        LOGGER.info("Alert: {} is in '{}' state since {} min(s)", alertName, currentState,
                                ClusterUtils.TIME_FORMAT.format((double) elapsedTime / ClusterUtils.MIN_IN_MS));
                        if (isPeriodReached(alert, elapsedTime) && isPolicyAttached(alert)) {
                            publishEvent(new ScalingEvent(alert));
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve alert history", e);
            publishEvent(new UpdateFailedEvent(clusterId));
        }
    }

    private boolean isAlertStateMet(String currentState, MetricAlert alert) {
        return currentState.equalsIgnoreCase(alert.getAlertState().getValue());
    }

    private long getPeriod(Map<String, Object> history) {
        return System.currentTimeMillis() - (long) history.get(ALERT_TS);
    }

    private boolean isPeriodReached(MetricAlert alert, float period) {
        return period > alert.getPeriod() * ClusterUtils.MIN_IN_MS;
    }

    private boolean isPolicyAttached(BaseAlert alert) {
        return alert.getScalingPolicy() != null;
    }

}
