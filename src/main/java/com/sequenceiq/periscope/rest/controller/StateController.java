package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.rest.ClusterNotFoundException;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.StateJson;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/state/{id}")
public class StateController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterConverter clusterConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> setState(@PathVariable String id, @RequestBody StateJson stateJson) {
        Cluster cluster = clusterService.setState(id, stateJson.getState());
        if (cluster == null) {
            throw new ClusterNotFoundException(id);
        }
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }
}
