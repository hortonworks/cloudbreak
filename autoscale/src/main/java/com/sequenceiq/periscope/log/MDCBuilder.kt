package com.sequenceiq.periscope.log

import org.slf4j.MDC

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.domain.PeriscopeUser

object MDCBuilder {

    @JvmOverloads fun buildMdcContext(cluster: Cluster? = null) {
        if (cluster == null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), "periscope")
        } else {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.user.id)
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cluster.id.toString())
            MDC.put(LoggerContextKey.CB_STACK_ID.toString(), cluster.stackId.toString())
        }
    }

    fun buildMdcContext(user: PeriscopeUser, clusterId: Long?) {
        buildUserMdcContext(user)
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), clusterId.toString())
    }

    fun buildUserMdcContext(user: PeriscopeUser?) {
        if (user != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), user.id)
        }
    }

}
