package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.model.AutoScalingGroup;
import com.sequenceiq.periscope.rest.converter.AutoScalingGroupConverter;
import com.sequenceiq.periscope.rest.json.AutoScalingGroupJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/policies")
public class PolicyController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private AutoScalingGroupConverter scalingGroupConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<AutoScalingGroupJson> setScaling(@PathVariable String clusterId, @RequestBody AutoScalingGroupJson autoScaling)
            throws ClusterNotFoundException {
        AutoScalingGroup scalingGroup = scalingGroupConverter.convert(autoScaling, clusterId);
        scalingService.setAutoScalingGroup(clusterId, scalingGroup);
        return new ResponseEntity<>(autoScaling, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<AutoScalingGroupJson> getScaling(@PathVariable String clusterId) throws ClusterNotFoundException {
        AutoScalingGroup autoScalingGroup = scalingService.getAutoScalingGroup(clusterId);
        AutoScalingGroupJson json = scalingGroupConverter.convert(autoScalingGroup);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

}
