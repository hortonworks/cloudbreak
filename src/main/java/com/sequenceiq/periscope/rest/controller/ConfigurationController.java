package com.sequenceiq.periscope.rest.controller;

import java.util.Map;

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
import com.sequenceiq.periscope.registry.QueueSetupException;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.converter.QueueSetupConverter;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.QueueSetupJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterConverter clusterConverter;
    @Autowired
    private QueueSetupConverter queueSetupConverter;

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> refreshConfiguration(@PathVariable String id) {
        ResponseEntity response;
        try {
            Cluster cluster = clusterService.refreshConfiguration(id);
            response = new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
        } catch (ConnectionException | ClusterNotFoundException e) {
            LOGGER.error("Error refreshing the configuration on cluster " + id, e);
            response = new ResponseEntity<>(ClusterJson.emptyJson().withId(id), HttpStatus.REQUEST_TIMEOUT);
        }
        return response;
    }

    @RequestMapping(value = "/{id}/queue", method = RequestMethod.POST)
    public ResponseEntity<QueueSetupJson> setQueueConfig(@PathVariable String id, @RequestBody QueueSetupJson queueSetup) {
        ResponseEntity<QueueSetupJson> result;
        try {
            Map<String, String> newSetup = clusterService.setQueueSetup(id, queueSetupConverter.convert(queueSetup));
            QueueSetupJson responseJson = new QueueSetupJson("Queue setup successfully applied", queueSetup.getNewSetup(), newSetup);
            result = new ResponseEntity<>(responseJson, HttpStatus.OK);
        } catch (QueueSetupException e) {
            result = new ResponseEntity<>(
                    new QueueSetupJson(e.getMessage(), queueSetup.getNewSetup(), e.getProperties()), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ClusterNotFoundException e) {
            result = new ResponseEntity<>(QueueSetupJson.emptyJson().withMessage("Cluster not found"), HttpStatus.NOT_FOUND);
        }
        return result;
    }
}
