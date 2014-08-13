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
import com.sequenceiq.periscope.rest.ClusterNotFoundException;
import com.sequenceiq.periscope.rest.converter.AppReportConverter;
import com.sequenceiq.periscope.rest.converter.ClusterConverter;
import com.sequenceiq.periscope.rest.json.AppMovementJson;
import com.sequenceiq.periscope.rest.json.AppReportJson;
import com.sequenceiq.periscope.rest.json.ClusterJson;
import com.sequenceiq.periscope.service.AppService;

@RestController
public class AppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class);

    @Autowired
    private AppService appService;
    @Autowired
    private AppReportConverter appReportConverter;
    @Autowired
    private ClusterConverter clusterConverter;

    @RequestMapping(value = "/random", method = RequestMethod.POST)
    public ResponseEntity<String> randomize(@PathVariable String clusterId) {
        appService.setPriorityToHighRandomly(clusterId);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @RequestMapping(value = "/applications/{clusterId}", method = RequestMethod.GET)
    public ResponseEntity<List<AppReportJson>> getApp(@PathVariable String clusterId) {
        List<AppReportJson> result = new ArrayList<>();
        HttpStatus status;
        try {
            result.addAll(appReportConverter.convertAllToJson(appService.getApplicationReports(clusterId)));
            status = HttpStatus.OK;
        } catch (Exception e) {
            LOGGER.error("Error reporting apps", e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(result, status);
    }

    @RequestMapping(value = "/movement/{clusterId}", method = RequestMethod.POST)
    public ResponseEntity<ClusterJson> enableMovement(@PathVariable String clusterId, @RequestBody AppMovementJson appMovementJson) {
        Cluster cluster = appService.allowAppMovement(clusterId, appMovementJson.isAllowed());
        if (cluster == null) {
            throw new ClusterNotFoundException(clusterId);
        }
        return new ResponseEntity<>(clusterConverter.convert(cluster), HttpStatus.OK);
    }

}
