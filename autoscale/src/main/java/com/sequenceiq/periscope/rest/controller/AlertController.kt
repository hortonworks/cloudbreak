package com.sequenceiq.periscope.rest.controller

import java.text.ParseException
import java.util.ArrayList

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.endpoint.AlertEndpoint
import com.sequenceiq.periscope.api.model.MetricAlertJson
import com.sequenceiq.periscope.api.model.TimeAlertJson
import com.sequenceiq.periscope.domain.MetricAlert
import com.sequenceiq.periscope.domain.TimeAlert
import com.sequenceiq.periscope.rest.converter.MetricAlertConverter
import com.sequenceiq.periscope.rest.converter.TimeAlertConverter
import com.sequenceiq.periscope.service.AlertService
import com.sequenceiq.periscope.utils.DateUtils

@Component
class AlertController : AlertEndpoint {

    @Autowired
    private val alertService: AlertService? = null
    @Autowired
    private val metricAlertConverter: MetricAlertConverter? = null
    @Autowired
    private val timeAlertConverter: TimeAlertConverter? = null

    override fun createAlerts(clusterId: Long?, json: MetricAlertJson): MetricAlertJson {
        val metricAlert = metricAlertConverter!!.convert(json)
        return createMetricAlarmResponse(alertService!!.createMetricAlert(clusterId!!, metricAlert))
    }

    override fun updateAlerts(clusterId: Long?, alertId: Long?, json: MetricAlertJson): MetricAlertJson {
        val metricAlert = metricAlertConverter!!.convert(json)
        return createMetricAlarmResponse(alertService!!.updateMetricAlert(clusterId!!, alertId!!, metricAlert))
    }

    @Transactional
    override fun getAlerts(clusterId: Long?): List<MetricAlertJson> {
        return createAlarmsResponse(alertService!!.getMetricAlerts(clusterId!!))
    }

    override fun deleteAlarm(clusterId: Long?, alertId: Long?) {
        alertService!!.deleteMetricAlert(clusterId!!, alertId!!)
    }

    override fun getAlertDefinitions(clusterId: Long?): List<Map<String, Any>> {
        return alertService!!.getAlertDefinitions(clusterId!!)
    }

    @Throws(ParseException::class)
    override fun createTimeAlert(clusterId: Long?, json: TimeAlertJson): TimeAlertJson {
        val timeAlert = validateTimeAlert(json)
        return createTimeAlertResponse(alertService!!.createTimeAlert(clusterId!!, timeAlert))
    }

    @Throws(ParseException::class)
    override fun setTimeAlert(clusterId: Long?, alertId: Long?, json: TimeAlertJson): TimeAlertJson {
        val timeAlert = validateTimeAlert(json)
        return createTimeAlertResponse(alertService!!.updateTimeAlert(clusterId!!, alertId!!, timeAlert))
    }

    override fun getTimeAlerts(clusterId: Long?): List<TimeAlertJson> {
        val timeAlerts = alertService!!.getTimeAlerts(clusterId!!)
        return createTimeAlertsResponse(timeAlerts)
    }

    override fun deleteTimeAlert(clusterId: Long?, alertId: Long?) {
        alertService!!.deleteTimeAlert(clusterId!!, alertId!!)
    }

    @Throws(ParseException::class)
    private fun validateTimeAlert(json: TimeAlertJson): TimeAlert {
        val alert = timeAlertConverter!!.convert(json)
        DateUtils.getCronExpression(alert.cron)
        return alert
    }

    private fun createAlarmsResponse(alerts: Set<MetricAlert>): List<MetricAlertJson> {
        val metricAlerts = ArrayList<MetricAlert>()
        metricAlerts.addAll(alerts)
        return metricAlertConverter!!.convertAllToJson(metricAlerts)
    }

    private fun createTimeAlertsResponse(alarms: Set<TimeAlert>): List<TimeAlertJson> {
        val metricAlarms = ArrayList<TimeAlert>()
        metricAlarms.addAll(alarms)
        return createTimeAlertsResponse(metricAlarms)
    }

    private fun createMetricAlarmResponse(alert: MetricAlert): MetricAlertJson {
        return metricAlertConverter!!.convert(alert)
    }

    private fun createTimeAlertResponse(alarm: TimeAlert): TimeAlertJson {
        return timeAlertConverter!!.convert(alarm)
    }

    private fun createTimeAlertsResponse(alarms: List<TimeAlert>): List<TimeAlertJson> {
        return timeAlertConverter!!.convertAllToJson(alarms)
    }
}
