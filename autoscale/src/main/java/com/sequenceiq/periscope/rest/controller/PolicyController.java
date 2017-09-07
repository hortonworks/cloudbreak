package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.api.endpoint.PolicyEndpoint;
import com.sequenceiq.periscope.api.model.ScalingPolicyJson;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController implements PolicyEndpoint {

    @Autowired
    private ScalingService scalingService;

    @Autowired
    private ScalingPolicyConverter policyConverter;

    @Override
    public ScalingPolicyJson addScaling(Long clusterId, ScalingPolicyJson json) {
        ScalingPolicy scalingPolicy = policyConverter.convert(json);
        return createScalingPolicyJsonResponse(scalingService.createPolicy(clusterId, json.getAlertId(), scalingPolicy));
    }

    @Override
    public ScalingPolicyJson setScaling(Long clusterId, Long policyId, ScalingPolicyJson scalingPolicy) {
        ScalingPolicy policy = policyConverter.convert(scalingPolicy);
        return createScalingPolicyJsonResponse(scalingService.updatePolicy(clusterId, policyId, policy));
    }

    @Override
    public List<ScalingPolicyJson> getScaling(Long clusterId) {
        return createScalingPoliciesJsonResponse(scalingService.getPolicies(clusterId));
    }

    @Override
    public void deletePolicy(Long clusterId, Long policyId) {
        scalingService.deletePolicy(clusterId, policyId);
    }

    private List<ScalingPolicyJson> createScalingPoliciesJsonResponse(List<ScalingPolicy> scalingPolicies) {
        return policyConverter.convertAllToJson(scalingPolicies);
    }

    private ScalingPolicyJson createScalingPolicyJsonResponse(ScalingPolicy scalingPolicy) {
        return policyConverter.convert(scalingPolicy);
    }

}
