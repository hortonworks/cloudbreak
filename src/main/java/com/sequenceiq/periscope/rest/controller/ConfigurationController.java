package com.sequenceiq.periscope.rest.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.ScalingConfigurationJson;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/clusters/{clusterId}/configurations")
public class ConfigurationController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterConverter clusterConverter;

    @RequestMapping(value = "/scaling", method = RequestMethod.POST)
    public ResponseEntity<ScalingConfigurationJson> setScalingConfiguration(@PathVariable long clusterId,
            @RequestBody @Valid ScalingConfigurationJson json) {
        clusterService.updateScalingConfiguration(clusterId, json);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    @RequestMapping(value = "/scaling", method = RequestMethod.GET)
    public ResponseEntity<ScalingConfigurationJson> getScalingConfiguration(@PathVariable long clusterId) {
        return new ResponseEntity<>(clusterService.getScalingConfiguration(clusterId), HttpStatus.OK);
    }

}
