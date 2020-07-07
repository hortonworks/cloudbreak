package com.sequenceiq.periscope.monitor.evaluator.load;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component("YarnScalingMetricsCollector")
@Scope("prototype")
public class YarnScalingMetricsCollector extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnScalingMetricsCollector.class);

    private static final String EVALUATOR_NAME = YarnScalingMetricsCollector.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private YarnAverageLoadMetrics yarnAverageLoadMetrics;

    private long clusterId;

    private Cluster cluster;

    private String policyHostGroup;

    private LoadAlert loadAlert;

    @Nonnull
    @Override
    public EvaluatorContext getContext() {
        return new ClusterIdEvaluatorContext(clusterId);
    }

    @Override
    public void setContext(EvaluatorContext context) {
        clusterId = (long) context.getData();
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    protected void execute() {
        long start = System.currentTimeMillis();
        String stackCrn = null;
        try {
            cluster = clusterService.findById(clusterId);
            MDCBuilder.buildMdcContext(cluster);
            stackCrn = cluster.getStackCrn();
            loadAlert = cluster.getLoadAlerts().stream().findFirst().get();
            policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

            pollYarnAndCollectMetrics();
        } catch (Exception ex) {
            LOGGER.info("Failed to process YarnScalingMetricsCollector for Cluster '{}', exception '{}'", stackCrn, ex);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished YarnScalingMetricsCollector for cluster '{}' in '{}' ms", stackCrn, System.currentTimeMillis() - start);
        }
    }

    protected void pollYarnAndCollectMetrics() throws Exception {
        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        Map<String, String> hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup);

        YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster, stackV4Response, policyHostGroup);
        Integer scalingRecommendation = yarnMetricsClient
                .getYarnRecommendedScaleUpCount(yarnResponse, policyHostGroup, loadAlert.getLoadAlertConfiguration().getMaxResourceValue())
                .orElse(-1 * yarnMetricsClient
                        .getYarnRecommendedDecommissionHosts(yarnResponse, hostFqdnsToInstanceId, Optional.empty()).size());
        yarnAverageLoadMetrics.addScalingRecommendation(cluster.getStackCrn(), scalingRecommendation);
    }
}