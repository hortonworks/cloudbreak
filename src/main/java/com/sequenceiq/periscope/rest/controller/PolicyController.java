package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.model.ScalingPolicies;
import com.sequenceiq.periscope.rest.converter.ScalingPoliciesConverter;
import com.sequenceiq.periscope.rest.json.ScalingPoliciesJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private ScalingPoliciesConverter policiesConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ScalingPoliciesJson> setScaling(@PathVariable long clusterId,
            @RequestBody ScalingPoliciesJson scalingPolicies) throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.setScalingPolicies(clusterId,
                policiesConverter.convert(scalingPolicies)), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<ScalingPoliciesJson> getScaling(@PathVariable long clusterId) throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.getScalingPolicies(clusterId));
    }

    @RequestMapping(value = "/{policyId}", method = RequestMethod.DELETE)
    public ResponseEntity<ScalingPoliciesJson> deletePolicy(@PathVariable long clusterId, @PathVariable long policyId)
            throws ClusterNotFoundException {
        return createScalingPoliciesJsonResponse(scalingService.deletePolicy(clusterId, policyId));
    }

    private ResponseEntity<ScalingPoliciesJson> createScalingPoliciesJsonResponse(ScalingPolicies scalingPolicies) {
        return createScalingPoliciesJsonResponse(scalingPolicies, HttpStatus.OK);
    }

    private ResponseEntity<ScalingPoliciesJson> createScalingPoliciesJsonResponse(ScalingPolicies scalingPolicies, HttpStatus status) {
        return new ResponseEntity<>(policiesConverter.convert(scalingPolicies), status);
    }

}
