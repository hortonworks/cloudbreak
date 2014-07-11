package com.sequenceiq.periscope.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.rest.converter.AmbariConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.AmbariJson;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.IdJson;

@RestController
@RequestMapping("/clusters")
public class ClusterController {

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private AmbariConverter ambariConverter;
    @Autowired
    private ClusterConverter clusterConverter;

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<IdJson> addCluster(@PathVariable String id, @RequestBody AmbariJson ambariServer) {
        clusterRegistry.add(id, ambariConverter.convert(ambariServer));
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@PathVariable String id) {
        ClusterRegistration cluster = clusterRegistry.get(id);
        if (cluster == null) {
            return new ResponseEntity<>(new ClusterJson("", "", ""), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ClusterJson>> getClusters() {
        List<ClusterJson> clusters = new ArrayList<>();
        Collection<ClusterRegistration> registrations = clusterRegistry.getAll();
        clusters.addAll(clusterConverter.convertAllToJson(registrations));
        return new ResponseEntity<>(clusters, HttpStatus.OK);
    }

}
