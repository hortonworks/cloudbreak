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

import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.rest.converter.MetricAlarmConverter;
import com.sequenceiq.periscope.rest.converter.TimeAlarmConverter;
import com.sequenceiq.periscope.rest.json.MetricAlarmJson;
import com.sequenceiq.periscope.rest.json.MetricAlarmsJson;
import com.sequenceiq.periscope.rest.json.TimeAlarmJson;
import com.sequenceiq.periscope.rest.json.TimeAlarmsJson;
import com.sequenceiq.periscope.service.AlarmService;
import com.sequenceiq.periscope.service.ClusterNotFoundException;

@RestController
@RequestMapping("/clusters/{clusterId}/alarms")
public class AlarmController {

    @Autowired
    private AlarmService alarmService;
    @Autowired
    private MetricAlarmConverter metricAlarmConverter;
    @Autowired
    private TimeAlarmConverter timeAlarmConverter;

    @RequestMapping(value = "/metric", method = RequestMethod.POST)
    public ResponseEntity<MetricAlarmsJson> createAlarms(@PathVariable long clusterId, @RequestBody MetricAlarmsJson json)
            throws ClusterNotFoundException {
        List<MetricAlarm> metricAlarms = metricAlarmConverter.convertAllFromJson(json.getAlarms());
        return createAlarmsResponse(alarmService.setMetricAlarms(clusterId, metricAlarms), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.PUT)
    public ResponseEntity<MetricAlarmsJson> addAlarm(@PathVariable long clusterId, @RequestBody MetricAlarmJson json)
            throws ClusterNotFoundException {
        MetricAlarm metricAlarm = metricAlarmConverter.convert(json);
        return createAlarmsResponse(alarmService.addMetricAlarm(clusterId, metricAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.GET)
    public ResponseEntity<MetricAlarmsJson> getAlarms(@PathVariable long clusterId) throws ClusterNotFoundException {
        return createAlarmsResponse(alarmService.getMetricAlarms(clusterId));
    }

    @RequestMapping(value = "/metric/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<MetricAlarmsJson> deleteAlarm(@PathVariable long clusterId, @PathVariable long alarmId)
            throws ClusterNotFoundException {
        return createAlarmsResponse(alarmService.deleteMetricAlarm(clusterId, alarmId));
    }

    @RequestMapping(value = "/time", method = RequestMethod.POST)
    public ResponseEntity<TimeAlarmsJson> createTimeAlarms(@PathVariable long clusterId, @RequestBody TimeAlarmsJson json)
            throws ClusterNotFoundException {
        List<TimeAlarm> alarms = timeAlarmConverter.convertAllFromJson(json.getAlarms());
        return createTimeAlarmsResponse(alarmService.setTimeAlarms(clusterId, alarms), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time", method = RequestMethod.PUT)
    public ResponseEntity<TimeAlarmsJson> addTimeAlarm(@PathVariable long clusterId, @RequestBody TimeAlarmJson json)
            throws ClusterNotFoundException {
        TimeAlarm timeAlarm = timeAlarmConverter.convert(json);
        return createTimeAlarmsResponse(alarmService.addTimeAlarm(clusterId, timeAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time", method = RequestMethod.GET)
    public ResponseEntity<TimeAlarmsJson> getTimeAlarms(@PathVariable long clusterId) throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.getTimeAlarms(clusterId));
    }

    @RequestMapping(value = "/time/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<TimeAlarmsJson> deleteTimeAlarm(@PathVariable long clusterId, @PathVariable long alarmId)
            throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.deleteTimeAlarm(clusterId, alarmId));
    }

    private ResponseEntity<MetricAlarmsJson> createAlarmsResponse(List<MetricAlarm> metricAlarms) {
        return createAlarmsResponse(metricAlarms, HttpStatus.OK);
    }

    private ResponseEntity<TimeAlarmsJson> createTimeAlarmsResponse(List<TimeAlarm> alarms) {
        return createTimeAlarmsResponse(alarms, HttpStatus.OK);
    }

    private ResponseEntity<MetricAlarmsJson> createAlarmsResponse(List<MetricAlarm> metricAlarms, HttpStatus status) {
        List<MetricAlarmJson> alarmResponse = metricAlarmConverter.convertAllToJson(metricAlarms);
        return new ResponseEntity<>(new MetricAlarmsJson(alarmResponse), status);
    }

    private ResponseEntity<TimeAlarmsJson> createTimeAlarmsResponse(List<TimeAlarm> alarms, HttpStatus status) {
        List<TimeAlarmJson> alarmResponse = timeAlarmConverter.convertAllToJson(alarms);
        return new ResponseEntity<>(new TimeAlarmsJson(alarmResponse), status);
    }
}
