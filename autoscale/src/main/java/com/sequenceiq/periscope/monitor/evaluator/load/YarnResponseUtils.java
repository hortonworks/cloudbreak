package com.sequenceiq.periscope.monitor.evaluator.load;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            Map<String, String> hostFqdnsToInstanceId, int maxAllowedDownScale, Optional<Integer> mandatoryDownScaleCount, Integer maxScaleDownStepSize) {
        // yarnResponse may not have maxAllowedDownscale candidates. This is expected when using LoadBasedDownscaling
        // where a force downscale is not required. In this case downscaling is done based on yarn recommended count.
        Integer allowedDownscale = mandatoryDownScaleCount.orElse(maxAllowedDownScale);
        allowedDownscale = allowedDownscale > maxScaleDownStepSize ? maxScaleDownStepSize : allowedDownscale;
        List<String> decommissionNodes = yarnResponse.getScaleDownCandidates().orElse(List.of()).stream()
                .map(YarnScalingServiceV1Response.DecommissionCandidate::getNodeId)
                .map(nodeFqdn -> nodeFqdn.split(":")[0])
                .filter(s -> hostFqdnsToInstanceId.keySet().contains(s))
                .map(nodeFqdn -> hostFqdnsToInstanceId.get(nodeFqdn))
                .limit(allowedDownscale)
                .collect(Collectors.toList());

        return decommissionNodes;
    }

    public Integer getYarnRecommendedScaleUpCount(YarnScalingServiceV1Response yarnResponse, String policyHostGroup,
            Integer maxAllowedUpScale, Optional<Integer> mandatoryUpScaleCount, Integer maxScaleUpStepSize) {
        Integer yarnRecommendedCount = yarnResponse.getScaleUpCandidates()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates::getCandidates).orElse(List.of()).stream()
                .filter(candidate -> candidate.getModelName().equalsIgnoreCase(policyHostGroup))
                .findFirst()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate::getCount)
                .map(scaleUpCount -> IntStream.of(scaleUpCount, maxAllowedUpScale, maxScaleUpStepSize).min().getAsInt())
                .orElse(0);

        return mandatoryUpScaleCount
                .map(mandatoryCount -> Math.max(mandatoryCount, yarnRecommendedCount))
                .orElse(yarnRecommendedCount);
    }
}