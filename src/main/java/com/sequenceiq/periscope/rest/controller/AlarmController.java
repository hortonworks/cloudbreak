package com.sequenceiq.periscope.rest.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.model.Alarm;
import com.sequenceiq.periscope.rest.converter.AlarmConverter;
import com.sequenceiq.periscope.rest.json.AlarmJson;
import com.sequenceiq.periscope.rest.json.AlarmsJson;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.service.ScalingService;

@RestController
@RequestMapping("/clusters/{clusterId}/alarms")
public class AlarmController {

    @Autowired
    private ScalingService scalingService;
    @Autowired
    private AlarmConverter alarmConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<AlarmsJson> createAlarms(@PathVariable String clusterId, @RequestBody AlarmsJson alarms)
            throws ClusterNotFoundException {
        List<Alarm> metricAlarms = alarmConverter.convertAllFromJson(alarms.getAlarms(), clusterId);
        scalingService.setAlarms(clusterId, metricAlarms);
        return getAlarms(clusterId);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<AlarmsJson> getAlarms(@PathVariable String clusterId) throws ClusterNotFoundException {
        List<Alarm> alarms = scalingService.getAlarms(clusterId);
        List<AlarmJson> metrics = alarmConverter.convertAllToJson(alarms);
        AlarmsJson alarmsJson = new AlarmsJson();
        alarmsJson.setAlarms(metrics);
        return new ResponseEntity<>(alarmsJson, HttpStatus.OK);
    }
}
