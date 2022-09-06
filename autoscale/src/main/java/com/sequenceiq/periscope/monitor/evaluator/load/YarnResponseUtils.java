package com.sequenceiq.periscope.monitor.evaluator.load;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;

@Component
public class YarnResponseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnResponseUtils.class);

    public List<String> getYarnRecommendedDecommissionHostsForHostGroup(YarnScalingServiceV1Response yarnResponse,
            Map<String, String> hostFqdnsToInstanceId) {
        // yarnResponse may not have maxAllowedDownscale candidates. This is expected when using LoadBasedDownscaling
        // where a force downscale is not required. In this case downscaling is done based on yarn recommended count.

        return yarnResponse.getScaleDownCandidates().orElse(List.of()).stream()
                .map(YarnScalingServiceV1Response.DecommissionCandidate::getNodeId)
                .map(nodeFqdn -> nodeFqdn.split(":")[0])
                .filter(hostFqdnsToInstanceId::containsKey)
                .map(hostFqdnsToInstanceId::get)
                .collect(Collectors.toList());
    }

    public Integer getYarnRecommendedScaleUpCount(YarnScalingServiceV1Response yarnResponse, String policyHostGroup) {

        return yarnResponse.getScaleUpCandidates()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates::getCandidates).orElse(List.of()).stream()
                .filter(candidate -> candidate.getModelName().equalsIgnoreCase(policyHostGroup))
                .findFirst()
                .map(YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate::getCount)
                .orElse(0);
    }
}