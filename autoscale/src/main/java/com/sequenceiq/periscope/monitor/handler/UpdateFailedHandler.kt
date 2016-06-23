package com.sequenceiq.periscope.monitor.handler

import java.util.concurrent.ConcurrentHashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.api.model.ClusterState
import com.sequenceiq.periscope.log.MDCBuilder
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent
import com.sequenceiq.periscope.service.ClusterService

@Component
class UpdateFailedHandler : ApplicationListener<UpdateFailedEvent> {

    @Autowired
    private val clusterService: ClusterService? = null
    private val updateFailures = ConcurrentHashMap<Long, Int>()

    override fun onApplicationEvent(event: UpdateFailedEvent) {
        val id = event.clusterId
        val cluster = clusterService!!.find(id)
        MDCBuilder.buildMdcContext(cluster)
        val failed = updateFailures[id]
        if (failed == null) {
            updateFailures.put(id, 1)
        } else if (RETRY_THRESHOLD - 1 == failed) {
            cluster.state = ClusterState.SUSPENDED
            clusterService.save(cluster)
            updateFailures.remove(id)
            LOGGER.info("Suspend cluster monitoring due to failing update attempts")
        } else {
            updateFailures.put(id, failed + 1)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(UpdateFailedHandler::class.java)
        private val RETRY_THRESHOLD = 5
    }
}
