package com.sequenceiq.periscope.rest.controller;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.rest.converter.MetricAlertConverter;
import com.sequenceiq.periscope.rest.converter.TimeAlertConverter;
import com.sequenceiq.periscope.rest.json.MetricAlertJson;
import com.sequenceiq.periscope.rest.json.TimeAlertJson;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.utils.DateUtils;

@RestController
@RequestMapping("/clusters/{clusterId}/alerts")
public class AlertController {

    @Autowired
    private AlertService alertService;
    @Autowired
    private MetricAlertConverter metricAlertConverter;
    @Autowired
    private TimeAlertConverter timeAlertConverter;

    @RequestMapping(value = "/metric", method = RequestMethod.POST)
    public ResponseEntity<MetricAlertJson> createAlerts(@PathVariable long clusterId, @RequestBody @Valid MetricAlertJson json) {
        MetricAlert metricAlert = metricAlertConverter.convert(json);
        return createMetricAlarmResponse(alertService.createMetricAlert(clusterId, metricAlert), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/metric/{alertId}", method = RequestMethod.PUT)
    public ResponseEntity<MetricAlertJson> updateAlerts(@PathVariable long clusterId, @PathVariable long alertId, @RequestBody @Valid MetricAlertJson json) {
        MetricAlert metricAlert = metricAlertConverter.convert(json);
        return createMetricAlarmResponse(alertService.updateMetricAlert(clusterId, alertId, metricAlert), HttpStatus.OK);
    }

    @RequestMapping(value = "/metric", method = RequestMethod.GET)
    public ResponseEntity<List<MetricAlertJson>> getAlerts(@PathVariable long clusterId) {
        return createAlarmsResponse(alertService.getMetricAlerts(clusterId));
    }

    @RequestMapping(value = "/metric/{alertId}", method = RequestMethod.DELETE)
    public ResponseEntity<MetricAlertJson> deleteAlarm(@PathVariable long clusterId, @PathVariable long alertId) {
        alertService.deleteMetricAlert(clusterId, alertId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/metric/definitions", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, String>>> getAlertDefinitions(@PathVariable long clusterId) {
        return new ResponseEntity<>(alertService.getAlertDefinitions(clusterId), HttpStatus.OK);
    }

    @RequestMapping(value = "/time", method = RequestMethod.POST)
    public ResponseEntity<TimeAlertJson> createTimeAlert(@PathVariable long clusterId, @RequestBody @Valid TimeAlertJson json) throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.createTimeAlert(clusterId, timeAlert), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/time/{alertId}", method = RequestMethod.PUT)
    public ResponseEntity<TimeAlertJson> setTimeAlert(@PathVariable long clusterId, @PathVariable long alertId, @RequestBody @Valid TimeAlertJson json)
            throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.updateTimeAlert(clusterId, alertId, timeAlert), HttpStatus.OK);
    }

    @RequestMapping(value = "/time", method = RequestMethod.GET)
    public ResponseEntity<List<TimeAlertJson>> getTimeAlerts(@PathVariable long clusterId) {
        return createTimeAlertsResponse(alertService.getTimeAlerts(clusterId));
    }

    @RequestMapping(value = "/time/{alertId}", method = RequestMethod.DELETE)
    public ResponseEntity<TimeAlertJson> deleteTimeAlert(@PathVariable long clusterId, @PathVariable long alertId) {
        alertService.deleteTimeAlert(clusterId, alertId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private TimeAlert validateTimeAlert(TimeAlertJson json) throws ParseException {
        TimeAlert alert = timeAlertConverter.convert(json);
        DateUtils.getCronExpression(alert.getCron());
        return alert;
    }

    private ResponseEntity<List<MetricAlertJson>> createAlarmsResponse(List<MetricAlert> metricAlarms) {
        List<MetricAlertJson> alarmsJson = metricAlertConverter.convertAllToJson(metricAlarms);
        return new ResponseEntity<>(alarmsJson, HttpStatus.OK);
    }

    private ResponseEntity<List<TimeAlertJson>> createTimeAlertsResponse(List<TimeAlert> alarms) {
        return createTimeAlertsResponse(alarms, HttpStatus.OK);
    }

    private ResponseEntity<MetricAlertJson> createMetricAlarmResponse(MetricAlert alert, HttpStatus status) {
        MetricAlertJson alarmResponse = metricAlertConverter.convert(alert);
        return new ResponseEntity<>(alarmResponse, status);
    }

    private ResponseEntity<TimeAlertJson> createTimeAlertResponse(TimeAlert alarm, HttpStatus status) {
        TimeAlertJson alarmResponse = timeAlertConverter.convert(alarm);
        return new ResponseEntity<>(alarmResponse, status);
    }

    private ResponseEntity<List<TimeAlertJson>> createTimeAlertsResponse(List<TimeAlert> alarms, HttpStatus status) {
        List<TimeAlertJson> alarmResponse = timeAlertConverter.convertAllToJson(alarms);
        return new ResponseEntity<>(alarmResponse, status);
    }
}
