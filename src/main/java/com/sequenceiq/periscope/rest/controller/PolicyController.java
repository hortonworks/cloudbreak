package com.sequenceiq.periscope.rest.controller;

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
import com.sequenceiq.periscope.model.ScalingPolicies;
import com.sequenceiq.periscope.rest.converter.ScalingPoliciesConverter;
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter;
import com.sequenceiq.periscope.rest.json.ScalingConfigurationJson;
import com.sequenceiq.periscope.rest.json.ScalingPoliciesJson;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private ScalingPoliciesConverter policiesConverter;
    @Autowired
    private ScalingPolicyConverter policyConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ScalingPoliciesJson> addScaling(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId,
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
    public ResponseEntity<ScalingPoliciesJson> getScaling(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId)
            throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.getScalingPolicies(user, clusterId));
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.DELETE)
    public ResponseEntity<ScalingPoliciesJson> deletePolicy(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long policyId) throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.deletePolicy(user, clusterId, policyId));
    }

    @RequestMapping(value = "/configuration", method = RequestMethod.POST)
    public ResponseEntity<ScalingConfigurationJson> setConfiguration(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId,
            @RequestBody @Valid ScalingConfigurationJson json) throws ClusterNotFoundException {
        scalingService.setScalingConfiguration(user, clusterId, json);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    private ResponseEntity<ScalingPoliciesJson> createScalingPoliciesJsonResponse(ScalingPolicies scalingPolicies) {
        return createScalingPoliciesJsonResponse(scalingPolicies, HttpStatus.OK);
    }

    private ResponseEntity<ScalingPolicyJson> createScalingPolicyJsonResponse(ScalingPolicy scalingPolicy, HttpStatus status) {
        return new ResponseEntity<>(policyConverter.convert(scalingPolicy), status);
    }

    private ResponseEntity<ScalingPoliciesJson> createScalingPoliciesJsonResponse(ScalingPolicies scalingPolicies, HttpStatus status) {
        return new ResponseEntity<>(policiesConverter.convert(scalingPolicies), status);
    }

}
