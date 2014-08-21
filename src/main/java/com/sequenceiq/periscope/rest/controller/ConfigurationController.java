package com.sequenceiq.periscope.rest.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.registry.ConnectionException;
import com.sequenceiq.periscope.registry.QueueSetupException;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.converter.QueueSetupConverter;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.rest.json.QueueSetupJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ClusterService;

@RestController
@RequestMapping("/clusters/{clusterId}/configurations")
public class ConfigurationController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ClusterConverter clusterConverter;
    @Autowired
    private QueueSetupConverter queueSetupConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> refreshConfiguration(@PathVariable long clusterId)
            throws ConnectionException, ClusterNotFoundException {
        Cluster cluster = clusterService.refreshConfiguration(clusterId);
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

    @RequestMapping(value = "/queue", method = RequestMethod.POST)
    public ResponseEntity<QueueSetupJson> setQueueConfig(@PathVariable long clusterId, @RequestBody QueueSetupJson queueSetup)
            throws ClusterNotFoundException, QueueSetupException {
        Map<String, String> newSetup = clusterService.setQueueSetup(clusterId, queueSetupConverter.convert(queueSetup));
        QueueSetupJson responseJson = new QueueSetupJson(queueSetup.getSetup(), newSetup);
        return new ResponseEntity<>(responseJson, HttpStatus.OK);
    }
}
