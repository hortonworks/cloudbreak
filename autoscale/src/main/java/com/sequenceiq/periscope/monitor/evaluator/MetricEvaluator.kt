package com.sequenceiq.periscope.monitor.evaluator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.MetricAlert
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.monitor.event.ScalingEvent
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent
import com.sequenceiq.periscope.repository.MetricAlertRepository
import com.sequenceiq.periscope.service.ClusterService
import com.sequenceiq.periscope.utils.AmbariClientProvider
import com.sequenceiq.periscope.utils.ClusterUtils

@Component("MetricEvaluator")
@Scope("prototype")
class MetricEvaluator : AbstractEventPublisher(), EvaluatorExecutor {

    @Autowired
    private val clusterService: ClusterService? = null
    @Autowired
    private val alertRepository: MetricAlertRepository? = null
    @Autowired
    private val ambariClientProvider: AmbariClientProvider? = null

    private var clusterId: Long = 0

    override fun setContext(context: Map<String, Any>) {
        this.clusterId = context[EvaluatorContext.CLUSTER_ID.name] as Long
    }

    override fun run() {
        val cluster = clusterService!!.find(clusterId)
        MDCBuilder.buildMdcContext(cluster)
        val ambariClient = ambariClientProvider!!.createAmbariClient(cluster)
        try {
            for (alert in alertRepository!!.findAllByCluster(clusterId)) {
                val alertName = alert.name
                LOGGER.info("Checking metric based alert: '{}'", alertName)
                val alertHistory = ambariClient.getAlertHistory(alert.definitionName, 1)
                val historySize = alertHistory.size
                if (historySize > 1) {
                    LOGGER.debug("Multiple results found for alert: {}, probably HOST alert, ignoring now..", alertName)
                    continue
                }
                if (!alertHistory.isEmpty()) {
                    val history = alertHistory[0]
                    val currentState = history[ALERT_STATE] as String
                    if (isAlertStateMet(currentState, alert)) {
                        val elapsedTime = getPeriod(history)
                        LOGGER.info("Alert: {} is in '{}' state since {} min(s)", alertName, currentState,
                                ClusterUtils.TIME_FORMAT.format(elapsedTime.toDouble() / ClusterUtils.MIN_IN_MS))
                        if (isPeriodReached(alert, elapsedTime.toFloat()) && isPolicyAttached(alert)) {
                            publishEvent(ScalingEvent(alert))
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to retrieve alert history", e)
            publishEvent(UpdateFailedEvent(clusterId))
        }

    }

    private fun isAlertStateMet(currentState: String, alert: MetricAlert): Boolean {
        return currentState.equals(alert.alertState.value, ignoreCase = true)
    }

    private fun getPeriod(history: Map<String, Any>): Long {
        return System.currentTimeMillis() - history[ALERT_TS] as Long
    }

    private fun isPeriodReached(alert: MetricAlert, period: Float): Boolean {
        return period > alert.period * ClusterUtils.MIN_IN_MS
    }

    private fun isPolicyAttached(alert: BaseAlert): Boolean {
        return alert.scalingPolicy != null
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MetricEvaluator::class.java)
        private val ALERT_STATE = "state"
        private val ALERT_TS = "timestamp"
    }

}
