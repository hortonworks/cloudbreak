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
import com.sequenceiq.periscope.service.AlarmService;
import com.sequenceiq.periscope.service.ClusterNotFoundException;

@RestController
@RequestMapping("/clusters/{clusterId}/alarms")
public class AlarmController {

    @Autowired
    private AlarmService alarmService;
    @Autowired
    private AlarmConverter alarmConverter;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<AlarmsJson> createAlarms(@PathVariable String clusterId, @RequestBody AlarmsJson alarms)
            throws ClusterNotFoundException {
        List<Alarm> metricAlarms = alarmConverter.convertAllFromJson(alarms.getAlarms());
        List<AlarmJson> alarmResponse = alarmConverter.convertAllToJson(alarmService.setAlarms(clusterId, metricAlarms));
        return new ResponseEntity<>(new AlarmsJson(alarmResponse), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<AlarmsJson> getAlarms(@PathVariable String clusterId) throws ClusterNotFoundException {
        List<Alarm> alarms = alarmService.getAlarms(clusterId);
        List<AlarmJson> metrics = alarmConverter.convertAllToJson(alarms);
        return new ResponseEntity<>(new AlarmsJson(metrics), HttpStatus.OK);
    }
}
