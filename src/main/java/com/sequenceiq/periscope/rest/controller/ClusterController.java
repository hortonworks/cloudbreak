package com.sequenceiq.periscope.rest.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.AmbariJson;
import com.sequenceiq.periscope.rest.json.AppMovementJson;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.StateJson;
import com.sequenceiq.periscope.service.AppService;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/clusters")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AmbariConverter ambariConverter;
    @Autowired
    private ClusterConverter clusterConverter;
    @Autowired
    private AppService appService;

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> addCluster(@PathVariable String id, @RequestBody AmbariJson ambariServer)
            throws ConnectionException {
        Cluster cluster = clusterService.add(id, ambariConverter.convert(ambariServer));
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@PathVariable String id) throws ClusterNotFoundException {
        Cluster cluster = clusterService.get(id);
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ClusterJson>> getClusters() {
        List<ClusterJson> result = new ArrayList<>();
        List<Cluster> clusters = clusterService.getAll();
        result.addAll(clusterConverter.convertAllToJson(clusters));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ClusterJson> deleteCluster(@PathVariable String id) throws ClusterNotFoundException {
        Cluster cluster = clusterService.remove(id);
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/state", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> setState(@PathVariable String id, @RequestBody StateJson stateJson)
            throws ClusterNotFoundException {
        Cluster cluster = clusterService.setState(id, stateJson.getState());
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/movement", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> enableMovement(@PathVariable String id, @RequestBody AppMovementJson appMovementJson)
            throws ClusterNotFoundException {
        Cluster cluster = appService.allowAppMovement(id, appMovementJson.isAllowed());
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

}
