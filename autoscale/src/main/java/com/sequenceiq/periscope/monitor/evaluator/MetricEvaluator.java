package com.sequenceiq.periscope.monitor.evaluator;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
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

@Component("MetricEvaluator")
@Scope("prototype")
public class MetricEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricEvaluator.class);

    private static final String EVALUATOR_NAME = MetricEvaluator.class.getName();

    private static final String ALERT_STATE = "state";

    @Inject
    private ClusterService clusterService;

    @Inject
    private MetricAlertRepository alertRepository;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private MetricCondition metricCondition;

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
            List<MetricAlert> metricAlerts = alertRepository.findAllWithScalingPolicyByCluster(clusterId);
            LOGGER.info("Metric alerts for cluster [id: {}]: {}", clusterId, metricAlerts);
            List<MetricAlert> alerts = metricAlerts.stream()
                    .filter(ma -> metricCondition.isMetricAlertTriggered(ambariClient, ma))
                    .collect(Collectors.toList());
            if (!alerts.isEmpty()) {
                eventPublisher.publishEvent(new ScalingEvent(alerts));
            } else {
                LOGGER.info("All metric alerts are filtered for cluster: [id: {}]", clusterId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve alert history", e);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.info("Finished metricEvaluator for cluster {} in {} ms", clusterId, System.currentTimeMillis() - start);
        }
    }
}
