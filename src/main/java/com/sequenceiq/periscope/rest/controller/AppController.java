package com.sequenceiq.periscope.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.log.Logger;
import com.sequenceiq.periscope.log.PeriscopeLoggerFactory;
import com.sequenceiq.periscope.rest.converter.AppReportConverter;
import com.sequenceiq.periscope.rest.json.AppReportJson;
import com.sequenceiq.periscope.service.AppService;
import com.sequenceiq.periscope.service.ClusterNotFoundException;

@RestController
@RequestMapping("/clusters/{clusterId}/applications")
public class AppController {

    private static final Logger LOGGER = PeriscopeLoggerFactory.getLogger(AppController.class);

    @Autowired
    private AppService appService;
    @Autowired
    private AppReportConverter appReportConverter;

    @RequestMapping(value = "/random", method = RequestMethod.POST)
    public ResponseEntity<String> randomize(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId)
            throws ClusterNotFoundException {
        appService.setPriorityToHighRandomly(user, clusterId);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<AppReportJson>> getApp(@ModelAttribute("user") PeriscopeUser user, @PathVariable long clusterId) {
        ResponseEntity<List<AppReportJson>> result;
        try {
            List<ApplicationReport> reports = appService.getApplicationReports(user, clusterId);
            Collection<AppReportJson> jsonList = appReportConverter.convertAllToJson(reports);
            List<AppReportJson> list = new ArrayList<>(jsonList);
            result = new ResponseEntity<>(list, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(clusterId, "Error reporting apps", e);
            result = new ResponseEntity<>(Collections.<AppReportJson>emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

}
