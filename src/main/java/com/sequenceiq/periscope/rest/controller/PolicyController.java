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
    public ResponseEntity<ScalingPoliciesJson> setScaling(@PathVariable String clusterId,
            @RequestBody ScalingPoliciesJson scalingPolicies) throws ClusterNotFoundException {
        ScalingPolicies policies = scalingService.setScalingPolicies(clusterId, policiesConverter.convert(scalingPolicies));
        return new ResponseEntity<>(policiesConverter.convert(policies), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<ScalingPoliciesJson> getScaling(@PathVariable String clusterId) throws ClusterNotFoundException {
        ScalingPolicies scalingPolicies = scalingService.getScalingPolicies(clusterId);
        ScalingPoliciesJson json = policiesConverter.convert(scalingPolicies);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

}
