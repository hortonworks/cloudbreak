package com.sequenceiq.periscope.monitor.evaluator.load;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response.DecommissionCandidate;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response.NewNodeManagerCandidates;
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

    private long clusterId;

    private Cluster cluster;

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
        String stackCrn = "NotInitialized";
        try {
            MDCBuilder.buildMdcContext(cluster);
            cluster = clusterService.findById(clusterId);
            stackCrn = cluster.getStackCrn();
            loadAlert = cluster.getLoadAlerts().stream().findFirst().get();

            if (isCoolDownTimeElapsed(cluster.getStackCrn(),
                    loadAlert.getLoadAlertConfiguration().getCoolDownMillis(),
                    cluster.getLastScalingActivity())) {
                pollYarnMetricsAndScaleCluster();
            }
        } catch (Exception ex) {
            LOGGER.info("Failed to process load alert for Cluster {}, exception {}", stackCrn, ex);
            eventPublisher.publishEvent(new UpdateFailedEvent(clusterId));
        } finally {
            LOGGER.debug("Finished loadEvaluator for cluster {} in {} ms", stackCrn, System.currentTimeMillis() - start);
        }
    }

    protected void pollYarnMetricsAndScaleCluster() throws Exception {

        StackV4Response stackV4Response = cloudbreakCommunicator.getByCrn(cluster.getStackCrn());

        String hostGroupInstanceType =
                stackResponseUtils.getHostGroupInstanceType(stackV4Response, loadAlert.getScalingPolicy().getHostGroup());

        YarnScalingServiceV1Response yarnResponse = yarnMetricsClient
                .getYarnMetricsForCluster(cluster, hostGroupInstanceType, stackV4Response.getCloudPlatform());

        Map<String, String> hostFqdnsToInstanceId = stackResponseUtils
                .getCloudInstanceIdsForHostGroup(stackV4Response, loadAlert.getScalingPolicy().getHostGroup());
        yarnResponse.getScaleUpCandidates().ifPresentOrElse(
                scaleUpCandidates -> handleScaleUp(hostGroupInstanceType, scaleUpCandidates, hostFqdnsToInstanceId.size()),
                () -> {
                    yarnResponse.getScaleDownCandidates().ifPresent(
                            scaleDownCandidates -> handleScaleDown(scaleDownCandidates, hostFqdnsToInstanceId));
                });
    }

    protected void handleScaleUp(String hostGroupInstanceType, NewNodeManagerCandidates newNMCandidates, Integer existingHostGroupSize) {
        Integer yarnRecommendedHostGroupCount =
                newNMCandidates.getCandidates().stream()
                        .filter(candidate -> candidate.getModelName().equalsIgnoreCase(hostGroupInstanceType))
                        .findFirst()
                        .map(NewNodeManagerCandidates.Candidate::getCount)
                        .orElseThrow(() -> new RuntimeException(String.format(
                                "Yarn Scaling API Response does not contain recommended node count " +
                                        " for hostGroupInstanceType '%s' in Cluster '%s', Yarn Response '%s'",
                                hostGroupInstanceType, cluster.getStackCrn(), newNMCandidates)));

        Integer maxAllowedScaleUp = Math.max(0, loadAlert.getLoadAlertConfiguration().getMaxResourceValue() - existingHostGroupSize);
        Integer scaleUpCount = IntStream.of(yarnRecommendedHostGroupCount, DEFAULT_MAX_SCALE_UP_STEP_SIZE, maxAllowedScaleUp)
                .min()
                .getAsInt();

        LOGGER.info("ScaleUp NodeCount '{}' for Cluster '{}', HostGroup '{}'", scaleUpCount,
                cluster.getStackCrn(), loadAlert.getScalingPolicy().getHostGroup());

        if (scaleUpCount > 0) {
            ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
            scalingEvent.setHostGroupNodeCount(Optional.of(existingHostGroupSize));
            scalingEvent.setScaleUpNodeCount(Optional.of(scaleUpCount));
            eventPublisher.publishEvent(scalingEvent);
        }
    }

    protected void handleScaleDown(List<DecommissionCandidate> decommissionCandidates, Map<String, String> hostGroupFqdnsToInstanceId) {
        Set<String> hostGroupFqdns = hostGroupFqdnsToInstanceId.keySet();
        int maxAllowedScaleDown = Math.max(0, hostGroupFqdns.size() - loadAlert.getLoadAlertConfiguration().getMinResourceValue());

        List<String> decommissionHostGroupNodeIds = decommissionCandidates.stream()
                .sorted(Comparator.comparingInt(DecommissionCandidate::getAmCount))
                .map(DecommissionCandidate::getNodeId)
                .map(nodeFqdn -> nodeFqdn.split(":")[0])
                .filter(s -> hostGroupFqdns.contains(s))
                .limit(maxAllowedScaleDown)
                .map(nodeFqdn -> hostGroupFqdnsToInstanceId.get(nodeFqdn))
                .collect(Collectors.toList());

        LOGGER.info("ScaleDown NodeCount '{}' for Cluster '{}', HostGroup '{}', NodeIds '{}'",
                decommissionHostGroupNodeIds.size(), cluster.getStackCrn(), loadAlert.getScalingPolicy().getHostGroup(),
                decommissionHostGroupNodeIds);

        ScalingEvent scalingEvent = new ScalingEvent(loadAlert);
        if (!decommissionHostGroupNodeIds.isEmpty()) {
            scalingEvent.setDecommissionNodeIds(decommissionHostGroupNodeIds);
            eventPublisher.publishEvent(scalingEvent);
        }
    }
}
