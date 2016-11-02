package com.sequenceiq.periscope.rest.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.endpoint.AlertEndpoint;
import com.sequenceiq.periscope.api.model.MetricAlertJson;
import com.sequenceiq.periscope.api.model.TimeAlertJson;
import com.sequenceiq.periscope.domain.MetricAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.rest.converter.MetricAlertConverter;
import com.sequenceiq.periscope.rest.converter.TimeAlertConverter;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.utils.DateUtils;

@Component
public class AlertController implements AlertEndpoint {

    @Autowired
    private AlertService alertService;

    @Autowired
    private MetricAlertConverter metricAlertConverter;

    @Autowired
    private TimeAlertConverter timeAlertConverter;

    @Override
    public MetricAlertJson createAlerts(Long clusterId, MetricAlertJson json) {
        MetricAlert metricAlert = metricAlertConverter.convert(json);
        return createMetricAlarmResponse(alertService.createMetricAlert(clusterId, metricAlert));
    }

    @Override
    public MetricAlertJson updateAlerts(Long clusterId, Long alertId, MetricAlertJson json) {
        MetricAlert metricAlert = metricAlertConverter.convert(json);
        return createMetricAlarmResponse(alertService.updateMetricAlert(clusterId, alertId, metricAlert));
    }

    @Override
    @Transactional
    public List<MetricAlertJson> getAlerts(Long clusterId) {
        return createAlarmsResponse(alertService.getMetricAlerts(clusterId));
    }

    @Override
    public void deleteAlarm(Long clusterId, Long alertId) {
        alertService.deleteMetricAlert(clusterId, alertId);
    }

    @Override
    public List<Map<String, Object>> getAlertDefinitions(Long clusterId) {
        return alertService.getAlertDefinitions(clusterId);
    }

    @Override
    public TimeAlertJson createTimeAlert(Long clusterId, TimeAlertJson json) throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.createTimeAlert(clusterId, timeAlert));
    }

    @Override
    public TimeAlertJson setTimeAlert(Long clusterId, Long alertId, TimeAlertJson json)
            throws ParseException {
        TimeAlert timeAlert = validateTimeAlert(json);
        return createTimeAlertResponse(alertService.updateTimeAlert(clusterId, alertId, timeAlert));
    }

    @Override
    public List<TimeAlertJson> getTimeAlerts(Long clusterId) {
        Set<TimeAlert> timeAlerts = alertService.getTimeAlerts(clusterId);
        return createTimeAlertsResponse(timeAlerts);
    }

    @Override
    public void deleteTimeAlert(Long clusterId, Long alertId) {
        alertService.deleteTimeAlert(clusterId, alertId);
    }

    private TimeAlert validateTimeAlert(TimeAlertJson json) throws ParseException {
        TimeAlert alert = timeAlertConverter.convert(json);
        DateUtils.getCronExpression(alert.getCron());
        return alert;
    }

    private List<MetricAlertJson> createAlarmsResponse(Set<MetricAlert> alerts) {
        List<MetricAlert> metricAlerts = new ArrayList<>();
        metricAlerts.addAll(alerts);
        return metricAlertConverter.convertAllToJson(metricAlerts);
    }

    private List<TimeAlertJson> createTimeAlertsResponse(Set<TimeAlert> alarms) {
        List<TimeAlert> metricAlarms = new ArrayList<>();
        metricAlarms.addAll(alarms);
        return createTimeAlertsResponse(metricAlarms);
    }

    private MetricAlertJson createMetricAlarmResponse(MetricAlert alert) {
        return metricAlertConverter.convert(alert);
    }

    private TimeAlertJson createTimeAlertResponse(TimeAlert alarm) {
        return timeAlertConverter.convert(alarm);
    }

    private List<TimeAlertJson> createTimeAlertsResponse(List<TimeAlert> alarms) {
        return timeAlertConverter.convertAllToJson(alarms);
    }
}
