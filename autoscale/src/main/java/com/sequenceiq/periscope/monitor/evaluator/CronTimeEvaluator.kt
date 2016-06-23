package com.sequenceiq.periscope.monitor.evaluator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.TimeAlert
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.monitor.MonitorUpdateRate
import com.sequenceiq.periscope.monitor.event.ScalingEvent
import com.sequenceiq.periscope.repository.TimeAlertRepository
import com.sequenceiq.periscope.utils.DateUtils

@Component("CronTimeEvaluator")
@Scope("prototype")
class CronTimeEvaluator : AbstractEventPublisher(), EvaluatorExecutor {

    @Autowired
    private val alertRepository: TimeAlertRepository? = null

    private var clusterId: Long = 0

    override fun setContext(context: Map<String, Any>) {
        this.clusterId = context[EvaluatorContext.CLUSTER_ID.name] as Long
    }

    private fun isTrigger(alert: TimeAlert): Boolean {
        return DateUtils.isTrigger(alert.cron, alert.timeZone, MonitorUpdateRate.CLUSTER_UPDATE_RATE.toLong())
    }

    private fun isPolicyAttached(alert: BaseAlert): Boolean {
        return alert.scalingPolicy != null
    }

    override fun run() {
        for (alert in alertRepository!!.findAllByCluster(clusterId)) {
            MDCBuilder.buildMdcContext(alert.cluster)
            val alertName = alert.name
            LOGGER.info("Checking time based alert: '{}'", alertName)
            if (isTrigger(alert) && isPolicyAttached(alert)) {
                LOGGER.info("Time alert: '{}' triggers", alertName)
                publishEvent(ScalingEvent(alert))
                break
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CronTimeEvaluator::class.java)
    }
}
