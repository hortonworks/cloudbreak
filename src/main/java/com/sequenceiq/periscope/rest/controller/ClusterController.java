package com.sequenceiq.periscope.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
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
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.IdJson;
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

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<IdJson> addCluster(@PathVariable String id, @RequestBody AmbariJson ambariServer) {
        try {
            clusterService.add(id, ambariConverter.convert(ambariServer));
        } catch (ConnectionException e) {
            LOGGER.error("Error adding the ambari cluster {} to the registry", ambariServer.getHost(), e);
            return new ResponseEntity<>(IdJson.emptyJson(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ClusterJson> getCluster(@PathVariable String id) {
        Cluster cluster = clusterService.get(id);
        if (cluster == null) {
            return new ResponseEntity<>(ClusterJson.emptyJson(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ClusterJson>> getClusters() {
        List<ClusterJson> result = new ArrayList<>();
        Collection<Cluster> clusters = clusterService.getAll();
        result.addAll(clusterConverter.convertAllToJson(clusters));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ClusterJson> deleteCluster(@PathVariable String id) {
        Cluster cluster = clusterService.remove(id);
        if (cluster == null) {
            return new ResponseEntity<>(ClusterJson.emptyJson(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

}
