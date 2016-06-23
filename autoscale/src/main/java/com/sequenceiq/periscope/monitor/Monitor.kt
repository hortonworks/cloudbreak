package com.sequenceiq.periscope.monitor

import org.quartz.Job

import com.sequenceiq.periscope.domain.Cluster

interface Monitor : Job {

    val identifier: String

    val triggerExpression: String

    val evaluatorType: Class<Any>

    fun getContext(cluster: Cluster): Map<String, Any>

}
