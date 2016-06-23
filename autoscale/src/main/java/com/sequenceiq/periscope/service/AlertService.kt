package com.sequenceiq.periscope.service

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.MetricAlert
import com.sequenceiq.periscope.domain.TimeAlert
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.repository.ClusterRepository
import com.sequenceiq.periscope.repository.MetricAlertRepository
import com.sequenceiq.periscope.repository.TimeAlertRepository
import com.sequenceiq.periscope.utils.AmbariClientProvider

import freemarker.template.Configuration

@Service
class AlertService {

    @Autowired
    private val clusterRepository: ClusterRepository? = null
    @Autowired
    private val metricAlertRepository: MetricAlertRepository? = null
    @Autowired
    private val timeAlertRepository: TimeAlertRepository? = null
    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val freemarkerConfiguration: Configuration? = null
    @Autowired
    private val ambariClientProvider: AmbariClientProvider? = null

    fun createMetricAlert(clusterId: Long, alert: MetricAlert): MetricAlert {
        val cluster = clusterService!!.findOneByUser(clusterId)
        alert.cluster = cluster
        val metricAlert = metricAlertRepository!!.save(alert)
        cluster.addMetricAlert(metricAlert)
        clusterRepository!!.save(cluster)
        return metricAlert
    }

    fun updateMetricAlert(clusterId: Long, alertId: Long, metricAlert: MetricAlert): MetricAlert {
        val alert = findMetricAlertByCluster(clusterId, alertId)
        alert.name = metricAlert.name
        alert.definitionName = metricAlert.definitionName
        alert.period = metricAlert.period
        alert.description = metricAlert.description
        alert.alertState = metricAlert.alertState
        return metricAlertRepository!!.save(alert)
    }

    fun findMetricAlertByCluster(clusterId: Long, alertId: Long): MetricAlert {
        return metricAlertRepository!!.findByCluster(alertId, clusterId)
    }

    fun deleteMetricAlert(clusterId: Long, alertId: Long) {
        metricAlertRepository!!.findByCluster(alertId, clusterId)
        val cluster = clusterRepository!!.find(clusterId)
        cluster.metricAlerts = removeMetricAlert(cluster, alertId)
        metricAlertRepository.delete(alertId)
        clusterRepository.save(cluster)
    }

    fun removeMetricAlert(cluster: Cluster, alertId: Long): MutableSet<MetricAlert> {
        val metricAlerts = cluster.metricAlerts
        for (metricAlert in cluster.metricAlerts) {
            if (metricAlert.id == alertId) {
                metricAlerts.remove(metricAlert)
            }
        }
        return metricAlerts
    }

    fun getMetricAlerts(clusterId: Long): Set<MetricAlert> {
        val cluster = clusterService!!.findOneByUser(clusterId)
        return cluster.metricAlerts
    }

    fun createTimeAlert(clusterId: Long, alert: TimeAlert): TimeAlert {
        var alert = alert
        val cluster = clusterService!!.findOneByUser(clusterId)
        alert.cluster = cluster
        alert = timeAlertRepository!!.save(alert)
        cluster.addTimeAlert(alert)
        clusterRepository!!.save(cluster)
        return alert
    }

    fun findTimeAlertByCluster(clusterId: Long, alertId: Long): TimeAlert {
        return timeAlertRepository!!.findByCluster(alertId, clusterId)
    }

    fun updateTimeAlert(clusterId: Long, alertId: Long, timeAlert: TimeAlert): TimeAlert {
        val alert = timeAlertRepository!!.findByCluster(alertId, clusterId)
        alert.description = timeAlert.description
        alert.cron = timeAlert.cron
        alert.timeZone = timeAlert.timeZone
        alert.name = timeAlert.name
        return timeAlertRepository.save(alert)
    }

    fun getTimeAlerts(clusterId: Long): Set<TimeAlert> {
        val cluster = clusterService!!.findOneByUser(clusterId)
        return cluster.timeAlerts
    }

    fun deleteTimeAlert(clusterId: Long, alertId: Long) {
        val cluster = clusterService!!.findOneByUser(clusterId)
        timeAlertRepository!!.findByCluster(alertId, clusterId)
        cluster.timeAlerts = removeTimeAlert(cluster, alertId)
        timeAlertRepository.delete(alertId)
        clusterRepository!!.save(cluster)
    }

    fun removeTimeAlert(cluster: Cluster, alertId: Long): MutableSet<TimeAlert> {
        val timeAlerts = cluster.timeAlerts
        for (timeAlert in cluster.timeAlerts) {
            if (timeAlert.id == alertId) {
                timeAlerts.remove(timeAlert)
            }
        }
        return timeAlerts
    }

    fun getBaseAlert(clusterId: Long, alertId: Long): BaseAlert {
        try {
            return findMetricAlertByCluster(clusterId, alertId)
        } catch (e: Exception) {
            return findTimeAlertByCluster(clusterId, alertId)
        }

    }

    fun save(alert: BaseAlert) {
        if (alert is MetricAlert) {
            metricAlertRepository!!.save(alert)
        } else {
            timeAlertRepository!!.save(alert as TimeAlert)
        }
    }

    fun getAlertDefinitions(clusterId: Long): List<Map<String, Any>> {
        val cluster = clusterService!!.findOneByUser(clusterId)
        val ret = ArrayList<Map<String, Any>>()
        val alertDefinitions = ambariClientProvider!!.createAmbariClient(cluster).alertDefinitions
        for (alertDefinition in alertDefinitions) {
            val tmp = HashMap<String, Any>()
            for (stringStringEntry in alertDefinition.entries) {
                tmp.put(stringStringEntry.key, stringStringEntry.value)
            }
            ret.add(tmp)
        }
        return ret
    }

    fun addPeriscopeAlerts(cluster: Cluster) {
        MDCBuilder.buildMdcContext(cluster)
        val client = ambariClientProvider!!.createAmbariClient(cluster)
        try {
            createAlert(client, getAlertDefinition(client, CONTAINER_ALERT), CONTAINER_ALERT)
            createAlert(client, getAlertDefinition(client, APP_ALERT), APP_ALERT)
        } catch (e: Exception) {
            LOGGER.error("Cannot parse alert definitions", e)
        }

    }

    @Throws(Exception::class)
    private fun getAlertDefinition(client: AmbariClient, name: String): String {
        val model = Collections.singletonMap("clusterName", client.clusterName)
        return processTemplateIntoString(freemarkerConfiguration!!.getTemplate(ALERT_PATH + name, "UTF-8"), model)
    }

    private fun createAlert(client: AmbariClient, json: String, alertName: String) {
        try {
            client.createAlert(json)
            LOGGER.info("Alert: {} added to the cluster", alertName)
        } catch (e: Exception) {
            LOGGER.info("Cannot add '{}' to the cluster", alertName)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AlertService::class.java)
        private val ALERT_PATH = "alerts/"
        private val CONTAINER_ALERT = "pending_containers.ftl"
        private val APP_ALERT = "pending_apps.ftl"
    }
}
