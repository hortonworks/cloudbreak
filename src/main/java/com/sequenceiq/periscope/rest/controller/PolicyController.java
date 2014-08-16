package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.policies.scaling.ScalingPolicy;
import com.sequenceiq.periscope.rest.converter.ScalingPolicyConverter;
import com.sequenceiq.periscope.rest.json.ScalingPolicyJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.PolicyService;

@RestController
@RequestMapping("/policy/{clusterId}")
public class PolicyController {

    @Autowired
    private PolicyService policyService;
    @Autowired
    private ScalingPolicyConverter scalingPolicyConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ScalingPolicyJson> createRules(@PathVariable String clusterId, @RequestBody ScalingPolicyJson policyJson)
            throws ClusterNotFoundException {
        ScalingPolicy policy = scalingPolicyConverter.convert(policyJson);
        policyService.setScalingPolicy(clusterId, policy);
        return new ResponseEntity<>(policyJson, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<ScalingPolicyJson> getPolicy(@PathVariable String clusterId) throws ClusterNotFoundException {
        ScalingPolicy policy = getCloudbreakPolicy(clusterId);
        ResponseEntity<ScalingPolicyJson> result;
        if (policy != null) {
            result = new ResponseEntity<>(scalingPolicyConverter.convert(policy), HttpStatus.OK);
        } else {
            result = new ResponseEntity<>(ScalingPolicyJson.emptyJson(), HttpStatus.NOT_FOUND);
        }
        return result;
    }

    private ScalingPolicy getCloudbreakPolicy(String clusterId) throws ClusterNotFoundException {
        return policyService.getScalingPolicy(clusterId);
    }

}
