package com.sequenceiq.periscope.monitor.evaluator.load;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;

@Component
public class YarnResponseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnResponseUtils.class);

    public List<String> getYarnRecommendedDecommissionHostsForHostGroup(String clusterCrn, YarnScalingServiceV1Response yarnResponse,
            Map<String, String> hostFqdnsToInstanceId, int maxAllowedDownScale, Optional<Integer> mandatoryDownScaleCount) {
        Set<String> consideredNodeIds = new HashSet<>();
        Integer allowedDownscale = Math.max(maxAllowedDownScale, mandatoryDownScaleCount.orElse(0));
        List<String> decommissionNodes = yarnResponse.getScaleDownCandidates().orElse(List.of()).stream()
                .sorted(Comparator.comparingInt(YarnScalingServiceV1Response.DecommissionCandidate::getAmCount))
                .map(YarnScalingServiceV1Response.DecommissionCandidate::getNodeId)
                .map(nodeFqdn -> nodeFqdn.split(":")[0])
                .filter(s -> hostFqdnsToInstanceId.keySet().contains(s))
                .map(nodeFqdn -> hostFqdnsToInstanceId.get(nodeFqdn))
                .limit(allowedDownscale)
                .map(nodeId -> {
                    consideredNodeIds.add(nodeId);
                    return nodeId;
                })
                .collect(Collectors.toList());

        //HostGroup candidates if mandatoryDownScaleCount is not met based on yarnResponse.
        int forcedCandidatesCount = mandatoryDownScaleCount.orElse(0) - decommissionNodes.size();
        if (forcedCandidatesCount > 0) {
            List forcedCandidates = hostFqdnsToInstanceId.keySet().stream()
                    .filter(nodeId -> !consideredNodeIds.contains(nodeId))
                    .limit(forcedCandidatesCount).collect(Collectors.toList());

            LOGGER.info("Forced downscaling candidates count '{}', candidates '{}' in cluster '{}' to achieve downscaletarget.",
                    forcedCandidates.size(), forcedCandidates, clusterCrn);
            decommissionNodes.addAll(forcedCandidates);
        }

        return decommissionNodes;
    }

    public Integer getYarnRecommendedScaleUpCount(YarnScalingServiceV1Response yarnResponse, String policyHostGroup,
            Integer maxAllowedUpScale, Optional<Integer> mandatoryUpScaleCount) {
        Integer yarnRecommendedCount = yarnResponse.getScaleUpCandidates()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates::getCandidates).orElse(List.of()).stream()
                .filter(candidate -> candidate.getModelName().equalsIgnoreCase(policyHostGroup))
                .findFirst()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate::getCount)
                .map(scaleUpCount -> IntStream.of(scaleUpCount, maxAllowedUpScale, DEFAULT_MAX_SCALE_UP_STEP_SIZE).min().getAsInt())
                .orElse(0);

        return mandatoryUpScaleCount
                .map(mandatoryCount -> Math.max(mandatoryCount, yarnRecommendedCount))
                .orElse(yarnRecommendedCount);
    }
}