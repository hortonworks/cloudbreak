package com.sequenceiq.cloudbreak.shell.util

import com.sequenceiq.cloudbreak.api.model.TopologyResponse

object TopologyUtil {

    fun checkTopologyForResource(publics: Set<TopologyResponse>?, topologyId: Long?, platform: String) {
        if (publics != null && topologyId != null) {
            var found = false
            for (t in publics) {
                if (t.id == topologyId) {
                    found = true
                    if (t.cloudPlatform == platform) {
                        return
                    } else {
                        throw RuntimeException("The selected platform belongs to a different cloudplatform.")
                    }
                }
            }
            if (!found) {
                throw RuntimeException("Not found platform with id: " + topologyId)
            }
        }
    }
}
