package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.AmbariJson;
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
    public ResponseEntity<ClusterJson> addCluster(@ModelAttribute("user") PeriscopeUser user,
            @RequestBody AmbariJson ambariServer) throws ConnectionException {
        return createClusterJsonResponse(clusterService.add(user, ambariConverter.convert(ambariServer)), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{clusterId}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.get(user, clusterId));
    }

    @RequestMapping(value = "/{clusterId}", method = RequestMethod.DELETE)
    public ResponseEntity<ClusterJson> deleteCluster(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.remove(user, clusterId));
    }

    @RequestMapping(value = "/{clusterId}/state", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> setState(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody StateJson stateJson) throws ClusterNotFoundException {
        return createClusterJsonResponse(clusterService.setState(user, clusterId, stateJson.getState()));
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster) {
        return createClusterJsonResponse(cluster, HttpStatus.OK);
    }

    private ResponseEntity<ClusterJson> createClusterJsonResponse(Cluster cluster, HttpStatus status) {
        return new ResponseEntity<>(clusterConverter.convert(cluster), status);
    }

}
