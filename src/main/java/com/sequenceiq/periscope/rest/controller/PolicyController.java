package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private ScalingPolicyConverter policyConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<List<ScalingPolicyJson>> addScaling(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId,
            @RequestBody @Valid ScalingPolicyJson json) throws ClusterNotFoundException {
        ScalingPolicy scalingPolicy = policyConverter.convert(json);
        return createScalingPoliciesJsonResponse(scalingService.addScalingPolicy(user, clusterId, scalingPolicy), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.PUT)
    public ResponseEntity<ScalingPolicyJson> setScaling(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId,
            @PathVariable long policyId, @RequestBody @Valid ScalingPolicyJson scalingPolicy) throws ClusterNotFoundException {
        ScalingPolicy policy = policyConverter.convert(scalingPolicy);
        return createScalingPolicyJsonResponse(scalingService.setScalingPolicy(user, clusterId, policyId, policy), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ScalingPolicyJson>> getScaling(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId)
            throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.getScalingPolicies(user, clusterId), HttpStatus.OK);
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.DELETE)
    public ResponseEntity<List<ScalingPolicyJson>> deletePolicy(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long policyId) throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.deletePolicy(user, clusterId, policyId), HttpStatus.OK);
    }

    private ResponseEntity<List<ScalingPolicyJson>> createScalingPoliciesJsonResponse(List<ScalingPolicy> scalingPolicies, HttpStatus status) {
        return new ResponseEntity<>(policyConverter.convertAllToJson(scalingPolicies), status);
    }

    private ResponseEntity<ScalingPolicyJson> createScalingPolicyJsonResponse(ScalingPolicy scalingPolicy, HttpStatus status) {
        return new ResponseEntity<>(policyConverter.convert(scalingPolicy), status);
    }

}
