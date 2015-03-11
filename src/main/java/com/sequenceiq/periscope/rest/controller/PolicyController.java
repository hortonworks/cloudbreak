package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private ScalingPolicyConverter policyConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ScalingPolicyJson> addScaling(@PathVariable long clusterId, @RequestBody @Valid ScalingPolicyJson json) {
        ScalingPolicy scalingPolicy = policyConverter.convert(json);
        return createScalingPolicyJsonResponse(scalingService.createPolicy(clusterId, json.getAlertId(), scalingPolicy), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.PUT)
    public ResponseEntity<ScalingPolicyJson> setScaling(@PathVariable long clusterId,
            @PathVariable long policyId, @RequestBody @Valid ScalingPolicyJson scalingPolicy) {
        ScalingPolicy policy = policyConverter.convert(scalingPolicy);
        return createScalingPolicyJsonResponse(scalingService.updatePolicy(clusterId, policyId, policy), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ScalingPolicyJson>> getScaling(@PathVariable long clusterId) {
        return createScalingPoliciesJsonResponse(scalingService.getPolicies(clusterId), HttpStatus.OK);
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.DELETE)
    public ResponseEntity<ScalingPolicyJson> deletePolicy(@PathVariable long clusterId, @PathVariable long policyId) {
        scalingService.deletePolicy(clusterId, policyId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<List<ScalingPolicyJson>> createScalingPoliciesJsonResponse(List<ScalingPolicy> scalingPolicies, HttpStatus status) {
        return new ResponseEntity<>(policyConverter.convertAllToJson(scalingPolicies), status);
    }

    private ResponseEntity<ScalingPolicyJson> createScalingPolicyJsonResponse(ScalingPolicy scalingPolicy, HttpStatus status) {
        return new ResponseEntity<>(policyConverter.convert(scalingPolicy), status);
    }

}
