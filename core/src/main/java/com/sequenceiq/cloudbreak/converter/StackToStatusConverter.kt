package com.sequenceiq.cloudbreak.converter

import java.util.HashMap

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack

@Component
class StackToStatusConverter : AbstractConversionServiceAwareConverter<Stack, Map<Any, Any>>() {

    override fun convert(source: Stack): Map<Any, Any> {
        val stackStatus = HashMap<String, Any>()
        stackStatus.put("id", source.id)
        stackStatus.put("status", source.status.name)
        stackStatus.put("statusReason", source.statusReason)
        val cluster = source.cluster
        if (cluster != null) {
            stackStatus.put("clusterStatus", cluster.status.name)
            stackStatus.put("clusterStatusReason", cluster.statusReason)
        }
        return stackStatus
    }
}
