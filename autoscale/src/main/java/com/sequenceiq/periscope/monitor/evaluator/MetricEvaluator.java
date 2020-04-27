package com.sequenceiq.periscope.monitor.evaluator;

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
            for (MetricAlert metricAlert : alertRepository.findAllWithScalingPolicyByCluster(clusterId)) {
                if (metricCondition.isMetricAlertTriggered(ambariClient, metricAlert)) {
                    eventPublisher.publishEvent(new ScalingEvent(metricAlert));
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve alert history", e);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.info("Finished metricEvaluator for cluster {} in {} ms", clusterId, System.currentTimeMillis() - start);
        }
    }
}
