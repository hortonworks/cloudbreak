package com.sequenceiq.periscope.rest.controller;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.MetricAlarm;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.TimeAlarm;
import com.sequenceiq.periscope.rest.converter.MetricAlarmConverter;
import com.sequenceiq.periscope.rest.converter.TimeAlarmConverter;
import com.sequenceiq.periscope.rest.json.MetricAlarmJson;
import com.sequenceiq.periscope.rest.json.MetricAlarmsJson;
import com.sequenceiq.periscope.rest.json.TimeAlarmJson;
import com.sequenceiq.periscope.rest.json.TimeAlarmsJson;
import com.sequenceiq.periscope.service.AlarmService;
import com.sequenceiq.periscope.service.ClusterNotFoundException;
import com.sequenceiq.periscope.utils.DateUtils;

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
    public ResponseEntity<MetricAlarmsJson> createAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody MetricAlarmsJson json) throws ClusterNotFoundException {
        List<MetricAlarm> metricAlarms = metricAlarmConverter.convertAllFromJson(json.getAlarms());
        return createAlarmsResponse(alarmService.setMetricAlarms(user, clusterId, metricAlarms), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.PUT)
    public ResponseEntity<MetricAlarmsJson> addAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody MetricAlarmJson json) throws ClusterNotFoundException {
        MetricAlarm metricAlarm = metricAlarmConverter.convert(json);
        return createAlarmsResponse(alarmService.addMetricAlarm(user, clusterId, metricAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.GET)
    public ResponseEntity<MetricAlarmsJson> getAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createAlarmsResponse(alarmService.getMetricAlarms(user, clusterId));
    }

    @RequestMapping(value = "/metric/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<MetricAlarmsJson> deleteAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId) throws ClusterNotFoundException {
        return createAlarmsResponse(alarmService.deleteMetricAlarm(user, clusterId, alarmId));
    }

    @RequestMapping(value = "/time", method = RequestMethod.POST)
    public ResponseEntity<TimeAlarmsJson> createTimeAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody TimeAlarmsJson json) throws ClusterNotFoundException, ParseException {
        List<TimeAlarm> alarms = timeAlarmConverter.convertAllFromJson(json.getAlarms());
        validateCronExpression(alarms);
        return createTimeAlarmsResponse(alarmService.setTimeAlarms(user, clusterId, alarms), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time", method = RequestMethod.PUT)
    public ResponseEntity<TimeAlarmsJson> addTimeAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody TimeAlarmJson json) throws ClusterNotFoundException, ParseException {
        TimeAlarm timeAlarm = timeAlarmConverter.convert(json);
        validateCronExpression(Arrays.asList(timeAlarm));
        return createTimeAlarmsResponse(alarmService.addTimeAlarm(user, clusterId, timeAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time", method = RequestMethod.GET)
    public ResponseEntity<TimeAlarmsJson> getTimeAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.getTimeAlarms(user, clusterId));
    }

    @RequestMapping(value = "/time/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<TimeAlarmsJson> deleteTimeAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId) throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.deleteTimeAlarm(user, clusterId, alarmId));
    }

    private void validateCronExpression(List<TimeAlarm> alarms) throws ParseException {
        for (TimeAlarm alarm : alarms) {
            DateUtils.getCronExpression(alarm.getCron());
        }
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
