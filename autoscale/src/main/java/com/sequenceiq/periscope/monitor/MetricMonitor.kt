package com.sequenceiq.periscope.monitor

import java.util.Collections

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext
import com.sequenceiq.periscope.monitor.evaluator.MetricEvaluator

@Component
class MetricMonitor : AbstractMonitor(), Monitor {

    override val identifier: String
        get() = "metric-monitor"

    override val triggerExpression: String
        get() = MonitorUpdateRate.METRIC_UPDATE_RATE_CRON

    override val evaluatorType: Class<Any>
        get() = MetricEvaluator::class.java

    override fun getContext(cluster: Cluster): Map<String, Any> {
        return Collections.singletonMap<String, Any>(EvaluatorContext.CLUSTER_ID.name, cluster.id)
    }
}
