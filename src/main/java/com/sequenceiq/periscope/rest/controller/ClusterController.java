package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.model.Cluster;
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

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AmbariConverter ambariConverter;
    @Autowired
    private ClusterConverter clusterConverter;
    @Autowired
    private AppService appService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> addCluster(@RequestBody AmbariJson ambariServer) throws ConnectionException {
        return createClusterJsonResponse(clusterService.add(ambariConverter.convert(ambariServer)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@PathVariable long id) throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.get(id));
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ClusterJson>> getClusters() {
        List<Cluster> clusters = clusterService.getAll();
        return new ResponseEntity<>(clusterConverter.convertAllToJson(clusters), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ClusterJson> deleteCluster(@PathVariable long id) throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.remove(id));
    }

    @RequestMapping(value = "/{id}/state", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> setState(@PathVariable long id, @RequestBody StateJson stateJson)
            throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.setState(id, stateJson.getState()));
    }

    @RequestMapping(value = "/{id}/movement", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> enableMovement(@PathVariable long id, @RequestBody AppMovementJson appMovementJson)
            throws ClusterNotFoundException {
        return createClusterJsonResponse(appService.allowAppMovement(id, appMovementJson.isAllowed()));
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster) {
        return createClusterJsonResponse(cluster, HttpStatus.OK);
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster, HttpStatus status) {
        return new ResponseEntity<>(clusterConverter.convert(cluster), status);
    }

}
