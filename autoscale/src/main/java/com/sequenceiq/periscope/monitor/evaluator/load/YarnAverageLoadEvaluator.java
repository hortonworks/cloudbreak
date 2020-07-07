package com.sequenceiq.periscope.monitor.evaluator.load;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

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
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component("YarnAverageLoadEvaluator")
@Scope("prototype")
public class YarnAverageLoadEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnAverageLoadEvaluator.class);

    private static final String EVALUATOR_NAME = YarnAverageLoadEvaluator.class.getName();

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

    private LoadAlert loadAlert;

    private LoadAlertConfiguration loadAlertConfiguration;

    private String policyHostGroup;

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
            loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
            policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

            if (isCoolDownTimeElapsed(cluster.getStackCrn(), loadAlertConfiguration.getCoolDownMillis(),
                    cluster.getLastScalingActivity())) {
                scaleClusterForYarnAverage();
            }
        } catch (Exception ex) {
            LOGGER.info("Failed to process load alert for Cluster '{}', exception '{}'", stackCrn, ex);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished loadEvaluator for cluster '{}' in '{}' ms", stackCrn, System.currentTimeMillis() - start);
        }
    }

    protected void scaleClusterForYarnAverage() throws Exception {
        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        Map<String, String> hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup);

        int existingHostGroupSize = hostFqdnsToInstanceId.size();
        int configMaxNodeCount = loadAlertConfiguration.getMaxResourceValue() - existingHostGroupSize;
        int configMinNodeCount = hostFqdnsToInstanceId.keySet().size() - loadAlertConfiguration.getMinResourceValue();
        int yarnAverageCount = yarnAverageLoadMetrics.getScalingAverage(cluster.getStackCrn());

        int targetScaleUpCount = 0;
        int targetScaleDownCount = 0;
        if (configMaxNodeCount > 0 && yarnAverageCount > 0) {
            //Yarn triggered upscaling within policy limits.
            targetScaleUpCount = IntStream.of(yarnAverageCount, configMaxNodeCount, DEFAULT_MAX_SCALE_UP_STEP_SIZE).min().getAsInt();
        } else if (configMinNodeCount > 0 && yarnAverageCount < 0) {
            //Yarn triggered downscaling within policy limits.
            targetScaleDownCount = Math.min(configMinNodeCount, -yarnAverageCount);
        } else if (configMinNodeCount < 0) {
            //Policy triggered downscaling.
            targetScaleUpCount = -configMinNodeCount;
        } else if (configMaxNodeCount < 0) {
            //Policy triggered upscaling.
            targetScaleDownCount = -configMaxNodeCount;
        }

        if (targetScaleUpCount > 0) {
            sendScaleUpEvent(existingHostGroupSize, targetScaleUpCount);
        } else if (targetScaleDownCount > 0) {
            List<String> yarnDecommisionHosts = getYarnRecommendedDecommissionHostsForHostGroup(
                    stackV4Response, hostFqdnsToInstanceId, targetScaleDownCount);
            sendScaleDownEvent(targetScaleDownCount, existingHostGroupSize, yarnDecommisionHosts);
        }
    }

    public void sendScaleUpEvent(Integer existingHostGroupSize, Integer targetScaleUpCount) {
        ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
        scalingEvent.setHostGroupNodeCount(Optional.of(existingHostGroupSize));
        scalingEvent.setScalingNodeCount(Optional.of(targetScaleUpCount));
        eventPublisher.publishEvent(scalingEvent);
        LOGGER.info("Triggered ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                cluster.getStackCrn(), targetScaleUpCount, policyHostGroup);
    }

    public void sendScaleDownEvent(Integer targetScaleDownCount, Integer existingHostGroupSize,
            List<String> yarnRecommendedDecommissionHosts) {
        ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
        scalingEvent.setHostGroupNodeCount(Optional.of(existingHostGroupSize));

        if (!yarnRecommendedDecommissionHosts.isEmpty()) {
            scalingEvent.setDecommissionNodeIds(yarnRecommendedDecommissionHosts);
        } else {
            scalingEvent.setScalingNodeCount(Optional.of(-targetScaleDownCount));
        }
        eventPublisher.publishEvent(scalingEvent);
        LOGGER.info("Triggered ScaleDown for Cluster '{}', NodeCount '{}', HostGroup '{}', DecommissionNodeIds '{}'",
                targetScaleDownCount, cluster.getStackCrn(), policyHostGroup, yarnRecommendedDecommissionHosts);
    }

    public List<String> getYarnRecommendedDecommissionHostsForHostGroup(StackV4Response stackV4Response,
            Map<String, String> hostFqdnsToInstanceId, int maxAllowedDownScale) throws Exception {
        YarnScalingServiceV1Response yarnResponse = yarnMetricsClient.getYarnMetricsForCluster(cluster, stackV4Response, policyHostGroup);
        return yarnMetricsClient
                .getYarnRecommendedDecommissionHosts(yarnResponse, hostFqdnsToInstanceId, Optional.of(maxAllowedDownScale));
    }
}