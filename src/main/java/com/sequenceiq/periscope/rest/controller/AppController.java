package com.sequenceiq.periscope.rest.controller;

import org.apache.hadoop.yarn.exceptions.YarnException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.rest.json.AppJson;
import com.sequenceiq.periscope.service.AppService;

@RestController
@RequestMapping("/clusters/{clusterId}/apps")
public class AppController {

    @Autowired
    private AppService appService;

    @RequestMapping(value = "/{appId}/move", method = RequestMethod.PUT)
    public ResponseEntity<AppJson> move(@PathVariable String clusterId,
            @PathVariable String appId, @RequestBody AppJson appJson) throws YarnException {
        String queue = appJson.getQueue();
        appService.moveToQueue(clusterId, appId, queue);
        return new ResponseEntity<>(new AppJson(appId, queue), HttpStatus.OK);
    }

}
