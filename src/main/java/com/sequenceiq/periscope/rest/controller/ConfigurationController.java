package com.sequenceiq.periscope.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.rest.ClusterNotFoundException;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterConverter clusterConverter;

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> refreshConfiguration(@PathVariable String id) {
        ResponseEntity response;
        try {
            Cluster cluster = clusterService.refreshConfiguration(id);
            if (cluster == null) {
                throw new ClusterNotFoundException(id);
            }
            response = new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
        } catch (ConnectionException e) {
            LOGGER.error("Error refreshing the configuration on cluster " + id, e);
            response = new ResponseEntity<>(ClusterJson.emptyJson().withId(id), HttpStatus.REQUEST_TIMEOUT);
        }
        return response;
    }
}
