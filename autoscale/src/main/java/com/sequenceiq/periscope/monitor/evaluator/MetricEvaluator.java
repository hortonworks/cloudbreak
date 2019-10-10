package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.ClusterUtils;
import com.sequenceiq.periscope.utils.TimeUtil;

@Component("MetricEvaluator")
@Scope("prototype")
public class MetricEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricEvaluator.class);

    private static final String EVALUATOR_NAME = MetricEvaluator.class.getName();

    private static final String ALERT_STATE = "state";

    private static final String ALERT_TS = "original_timestamp";

    @Inject
    private ClusterService clusterService;

    @Inject
    private MetricAlertRepository alertRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

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

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        try {
            Cluster cluster = clusterService.findById(clusterId);
            MDCBuilder.buildMdcContext(cluster);
            AmbariClient ambariClient = ambariClientProvider.createAmbariClient(cluster);
            for (MetricAlert metricAlert : alertRepository.findAllWithScalingPolicyByCluster(clusterId)) {
                String alertName = metricAlert.getName();
                LOGGER.info("Checking metric based alert: '{}'", alertName);
                List<Map<String, Object>> alerts = ambariRequestLogging.logging(() ->
                        ambariClient.getAlertByNameAndState(metricAlert.getDefinitionName(), metricAlert.getAlertState().getValue()), "alert");
                if (alerts.size() > 1) {
                    LOGGER.debug("Multiple results found for alert: {}, probably HOST alert, ignoring now..", alertName);
                    continue;
                }
                if (!alerts.isEmpty()) {
                    Map<String, Object> alert = alerts.get(0);
                    String currentState = (String) alert.get(ALERT_STATE);
                    long elapsedTime = getPeriod(alert);
                    LOGGER.info("Alert: {} is in '{}' state since {} min(s)", alertName, currentState,
                            ClusterUtils.TIME_FORMAT.format((double) elapsedTime / TimeUtil.MIN_IN_MS));
                    if (isPeriodReached(metricAlert, elapsedTime)) {
                        eventPublisher.publishEvent(new ScalingEvent(metricAlert));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve alert history", e);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.info("Finished metricEvaluator for cluster {} in {} ms", clusterId, System.currentTimeMillis() - start);
        }
    }

    private long getPeriod(Map<String, Object> history) {
        return System.currentTimeMillis() - (long) history.get(ALERT_TS);
    }

    private boolean isPeriodReached(MetricAlert alert, float period) {
        return period > alert.getPeriod() * TimeUtil.MIN_IN_MS;
    }
}
