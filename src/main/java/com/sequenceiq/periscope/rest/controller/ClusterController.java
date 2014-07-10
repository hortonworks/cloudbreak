package com.sequenceiq.periscope.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.registry.ClusterRegistry;
import com.sequenceiq.periscope.rest.json.AmbariJson;
import com.sequenceiq.periscope.rest.json.IdJson;

@RestController
@RequestMapping("/clusters")
public class ClusterController {

    @Autowired
    private ClusterRegistry clusterRegistry;
    @Autowired
    private ConversionService converter;

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<IdJson> addCluster(@PathVariable String id, @RequestBody AmbariJson ambariServer) {
        clusterRegistry.add(id, converter.convert(ambariServer, Ambari.class));
        return new ResponseEntity<>(new IdJson(id), HttpStatus.CREATED);
    }

}
