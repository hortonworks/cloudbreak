package com.sequenceiq.periscope.monitor.evaluator.load;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
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
import com.sequenceiq.periscope.repository.LoadAlertRepository;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.LoggingUtils;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component("YarnLoadEvaluator")
@Scope("prototype")
public class YarnLoadEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnLoadEvaluator.class);

    private static final String EVALUATOR_NAME = YarnLoadEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private LoadAlertRepository alertRepository;

    @Inject
    private EventPublisher eventPublisher;

    @Inject
    private YarnMetricsClient yarnMetricsClient;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private YarnResponseUtils yarnResponseUtils;

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
        LOGGER.info("ZZZ: YarnLoadEvaluator executing for clusterId: {}", clusterId);
        long start = System.currentTimeMillis();
        String stackCrn = null;
        try {
            cluster = clusterService.findById(clusterId);
            LOGGER.info("ZZZ: YarnLoadEvaluator executing for clusterId:{}, stackCrn: {}", clusterId, cluster.getStackCrn());
            LoggingUtils.buildMdcContext(cluster);
            stackCrn = cluster.getStackCrn();
            loadAlert = cluster.getLoadAlerts().stream().findFirst().get();
            loadAlertConfiguration = loadAlert.getLoadAlertConfiguration();
            policyHostGroup = loadAlert.getScalingPolicy().getHostGroup();

            if (isCoolDownTimeElapsed(cluster.getStackCrn(), "polled", loadAlertConfiguration.getPollingCoolDownMillis(),
                    cluster.getLastScalingActivity())) {
                pollYarnMetricsAndScaleCluster();
            }
        } catch (Exception ex) {
            LOGGER.info("Failed to process load alert for Cluster '{}', exception '{}'", stackCrn, ex);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished loadEvaluator for cluster '{}' in '{}' ms", stackCrn, System.currentTimeMillis() - start);
        }
    }

    protected void pollYarnMetricsAndScaleCluster() throws Exception {
        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());
        // TODO CB-14929: This needs to be carefully handled. The filter for STOPPED instances, if it does not apply correctl (e.g. nodes
        //  are in 'Services Starting' state, can result in upscale being skipped, or in some scenarios - unnecessary downscales.
        //  Sample log for when this happened (downscale by 1 instead of upscale by 5).
        //  (AS min set to 15, AS max set to 20: 15 nodes RUNNING. 5 nodes in SERVICE_STARTING state - ended up downscaling by 1)
        //  Sample log: hostFqdnsToInstanceId=20, configMaxNodeCount=0, configMinNodeCount=1, maxAllowedUpScale=0, maxAllowedDownScale=1
        //  At least protect against such unnecessary downscales.
        Map<String, String> hostFqdnsToInstanceId = stackResponseUtils.getCloudInstanceIdsForHostGroup(stackV4Response, policyHostGroup);

        int existingHostGroupSize = hostFqdnsToInstanceId.size();
        int configMaxNodeCount = loadAlertConfiguration.getMaxResourceValue() - existingHostGroupSize;
        int configMinNodeCount = hostFqdnsToInstanceId.keySet().size() - loadAlertConfiguration.getMinResourceValue();

        int maxAllowedUpScale = configMaxNodeCount < 0 ? 0 : configMaxNodeCount;
        int maxAllowedDownScale = configMinNodeCount > 0 ? configMinNodeCount : 0;
        LOGGER.info("ZZZ: Various counts: hostFqdnsToInstanceId={}, configMaxNodeCount={}, configMinNodeCount={}, maxAllowedUpScale={}, maxAllowedDownScale={}",
                existingHostGroupSize, configMaxNodeCount, configMinNodeCount, maxAllowedUpScale, maxAllowedDownScale);

        Optional<Integer> mandatoryUpScaleCount = Optional.of(configMinNodeCount)
                .filter(mandatoryUpScale -> mandatoryUpScale < 0).map(upscale -> -1 * upscale);

        Optional<Integer> mandatoryDownScaleCount = Optional.of(configMaxNodeCount)
                .filter(mandatoryDownscale -> mandatoryDownscale < 0).map(downscale -> -1 * downscale);

        YarnScalingServiceV1Response yarnResponse = yarnMetricsClient
                .getYarnMetricsForCluster(cluster, stackV4Response, policyHostGroup, mandatoryDownScaleCount);

        int yarnRecommendedScaleUpCount = yarnResponseUtils.getYarnRecommendedScaleUpCount(yarnResponse, policyHostGroup,
                maxAllowedUpScale, mandatoryUpScaleCount, loadAlertConfiguration.getMaxScaleUpStepSize());
        List<String> yarnRecommendedDecommissionHosts = yarnResponseUtils.
                getYarnRecommendedDecommissionHostsForHostGroup(cluster.getStackCrn(), yarnResponse,
                        hostFqdnsToInstanceId, maxAllowedDownScale, mandatoryDownScaleCount, loadAlertConfiguration.getMaxScaleDownStepSize());

        LOGGER.info("ZZZ: yarnRecommendedScaleUpCount={}, yarnRecommendedDecommssion={}", yarnRecommendedScaleUpCount, yarnRecommendedDecommissionHosts);

        if (yarnRecommendedScaleUpCount > 0 && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-up",
                loadAlertConfiguration.getScaleUpCoolDownMillis(), cluster.getLastScalingActivity()))  {
            sendScaleUpEvent(stackV4Response.getNodeCount(), existingHostGroupSize, yarnRecommendedScaleUpCount);
        } else if (!yarnRecommendedDecommissionHosts.isEmpty() && isCoolDownTimeElapsed(cluster.getStackCrn(), "scaled-down",
                loadAlertConfiguration.getScaleDownCoolDownMillis(), cluster.getLastScalingActivity()))  {
            sendScaleDownEvent(existingHostGroupSize, yarnRecommendedDecommissionHosts);
        }
    }

    public void sendScaleUpEvent(Integer existingClusterNodeCount, Integer existingHostGroupSize, Integer targetScaleUpCount) {
        ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
        scalingEvent.setExistingHostGroupNodeCount(existingHostGroupSize);
        scalingEvent.setExistingClusterNodeCount(existingClusterNodeCount);
        scalingEvent.setDesiredAbsoluteHostGroupNodeCount(existingHostGroupSize + targetScaleUpCount);
        eventPublisher.publishEvent(scalingEvent);
        LOGGER.info("Triggered ScaleUp for Cluster '{}', NodeCount '{}', HostGroup '{}'",
                cluster.getStackCrn(), targetScaleUpCount, policyHostGroup);
    }

    public void sendScaleDownEvent(Integer existingHostGroupSize, List<String> yarnRecommendedDecommissionHosts) {
        ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
        scalingEvent.setExistingHostGroupNodeCount(existingHostGroupSize);
        scalingEvent.setDesiredAbsoluteHostGroupNodeCount(existingHostGroupSize - yarnRecommendedDecommissionHosts.size());
        scalingEvent.setDecommissionNodeIds(yarnRecommendedDecommissionHosts);
        eventPublisher.publishEvent(scalingEvent);
        LOGGER.info("Triggered ScaleDown for Cluster '{}', NodeCount '{}', HostGroup '{}', DecommissionNodeIds '{}'",
                cluster.getStackCrn(), yarnRecommendedDecommissionHosts.size(), policyHostGroup, yarnRecommendedDecommissionHosts);
    }
}