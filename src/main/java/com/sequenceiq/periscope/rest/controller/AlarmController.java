package com.sequenceiq.periscope.rest.controller;

import java.text.ParseException;
import java.util.List;

import javax.validation.Valid;

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
import com.sequenceiq.periscope.rest.json.TimeAlarmJson;
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
    public ResponseEntity<List<MetricAlarmJson>> createAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody @Valid MetricAlarmJson json) throws ClusterNotFoundException {
        MetricAlarm metricAlarm = metricAlarmConverter.convert(json);
        return createMetricAlarmsResponse(alarmService.addMetricAlarm(user, clusterId, metricAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric/{alarmId}", method = RequestMethod.PUT)
    public ResponseEntity<MetricAlarmJson> setAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId, @RequestBody @Valid MetricAlarmJson json) throws ClusterNotFoundException {
        MetricAlarm metricAlarm = metricAlarmConverter.convert(json);
        return createMetricAlarmResponse(alarmService.setMetricAlarm(user, clusterId, alarmId, metricAlarm), HttpStatus.OK);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.GET)
    public ResponseEntity<List<MetricAlarmJson>> getAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createAlarmsResponse(alarmService.getMetricAlarms(user, clusterId));
    }

    @RequestMapping(value = "/metric/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<MetricAlarmJson> deleteAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId) throws ClusterNotFoundException {
        return createAlarmResponse(alarmService.deleteMetricAlarm(user, clusterId, alarmId));
    }

    @RequestMapping(value = "/time", method = RequestMethod.POST)
    public ResponseEntity<List<TimeAlarmJson>> createTimeAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @RequestBody @Valid TimeAlarmJson json) throws ClusterNotFoundException, ParseException {
        TimeAlarm timeAlarm = validateTimeAlarm(json);
        return createTimeAlarmsResponse(alarmService.addTimeAlarm(user, clusterId, timeAlarm), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time/{alarmId}", method = RequestMethod.PUT)
    public ResponseEntity<TimeAlarmJson> setTimeAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId, @RequestBody @Valid TimeAlarmJson json) throws ClusterNotFoundException, ParseException {
        TimeAlarm timeAlarm = validateTimeAlarm(json);
        return createTimeAlarmResponse(alarmService.setTimeAlarm(user, clusterId, alarmId, timeAlarm), HttpStatus.OK);
    }

    @RequestMapping(value = "/time", method = RequestMethod.GET)
    public ResponseEntity<List<TimeAlarmJson>> getTimeAlarms(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId) throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.getTimeAlarms(user, clusterId));
    }

    @RequestMapping(value = "/time/{alarmId}", method = RequestMethod.DELETE)
    public ResponseEntity<List<TimeAlarmJson>> deleteTimeAlarm(@ModelAttribute("user") PeriscopeUser user,
            @PathVariable long clusterId, @PathVariable long alarmId) throws ClusterNotFoundException {
        return createTimeAlarmsResponse(alarmService.deleteTimeAlarm(user, clusterId, alarmId));
    }

    private TimeAlarm validateTimeAlarm(TimeAlarmJson json) throws ParseException {
        TimeAlarm alarm = timeAlarmConverter.convert(json);
        validateCronExpression(alarm);
        return alarm;
    }

    private void validateCronExpression(TimeAlarm alarm) throws ParseException {
        DateUtils.getCronExpression(alarm.getCron());
    }

    private ResponseEntity<List<MetricAlarmJson>> createAlarmsResponse(List<MetricAlarm> metricAlarms) {
        return createMetricAlarmsResponse(metricAlarms, HttpStatus.OK);
    }

    private ResponseEntity<List<TimeAlarmJson>> createTimeAlarmsResponse(List<TimeAlarm> alarms) {
        return createTimeAlarmsResponse(alarms, HttpStatus.OK);
    }

    private ResponseEntity<MetricAlarmJson> createAlarmResponse(MetricAlarm metricAlarm) {
        MetricAlarmJson alarmResponse = metricAlarmConverter.convert(metricAlarm);
        return new ResponseEntity<>(alarmResponse, HttpStatus.OK);
    }

    private ResponseEntity<MetricAlarmJson> createMetricAlarmResponse(MetricAlarm metricAlarm, HttpStatus status) {
        MetricAlarmJson alarmResponse = metricAlarmConverter.convert(metricAlarm);
        return new ResponseEntity<>(alarmResponse, status);
    }

    private ResponseEntity<List<MetricAlarmJson>> createMetricAlarmsResponse(List<MetricAlarm> metricAlarms, HttpStatus status) {
        List<MetricAlarmJson> alarmResponse = metricAlarmConverter.convertAllToJson(metricAlarms);
        return new ResponseEntity<>(alarmResponse, status);
    }

    private ResponseEntity<TimeAlarmJson> createTimeAlarmResponse(TimeAlarm alarm, HttpStatus status) {
        TimeAlarmJson alarmResponse = timeAlarmConverter.convert(alarm);
        return new ResponseEntity<>(alarmResponse, status);
    }

    private ResponseEntity<List<TimeAlarmJson>> createTimeAlarmsResponse(List<TimeAlarm> alarms, HttpStatus status) {
        List<TimeAlarmJson> alarmResponse = timeAlarmConverter.convertAllToJson(alarms);
        return new ResponseEntity<>(alarmResponse, status);
    }
}
