package com.sequenceiq.periscope.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.api.endpoint.v1.PolicyEndpoint;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyResponse;
import com.sequenceiq.periscope.converter.ScalingPolicyRequestConverter;
import com.sequenceiq.periscope.converter.ScalingPolicyResponseConverter;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController implements PolicyEndpoint {

    @Autowired
    private ScalingService scalingService;

    @Autowired
    private ScalingPolicyRequestConverter policyRequestConverter;

    @Autowired
    private ScalingPolicyResponseConverter policyResponseConverter;

    @Override
    public ScalingPolicyResponse addScalingPolicy(Long clusterId, ScalingPolicyRequest json) {
        ScalingPolicy scalingPolicy = policyRequestConverter.convert(json);
        return createScalingPolicyJsonResponse(scalingService.createPolicy(clusterId, json.getAlertId(), scalingPolicy));
    }

    @Override
    public ScalingPolicyResponse updateScalingPolicy(Long clusterId, Long policyId, ScalingPolicyRequest scalingPolicy) {
        ScalingPolicy policy = policyRequestConverter.convert(scalingPolicy);
        return createScalingPolicyJsonResponse(scalingService.updatePolicy(clusterId, policyId, policy));
    }

    @Override
    public List<ScalingPolicyResponse> getScalingPolicies(Long clusterId) {
        return createScalingPoliciesJsonResponse(scalingService.getPolicies(clusterId));
    }

    @Override
    public void deleteScalingPolicy(Long clusterId, Long policyId) {
        scalingService.deletePolicy(clusterId, policyId);
    }

    private List<ScalingPolicyResponse> createScalingPoliciesJsonResponse(List<ScalingPolicy> scalingPolicies) {
        return policyResponseConverter.convertAllToJson(scalingPolicies);
    }

    private ScalingPolicyResponse createScalingPolicyJsonResponse(ScalingPolicy scalingPolicy) {
        return policyResponseConverter.convert(scalingPolicy);
    }

}
