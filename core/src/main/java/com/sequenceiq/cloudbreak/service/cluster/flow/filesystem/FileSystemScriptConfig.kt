package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem

import java.util.HashMap

import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterLifecycleEvent

class FileSystemScriptConfig(val scriptLocation: String, val clusterLifecycleEvent: ClusterLifecycleEvent, val executionType: ExecutionType) {
    val properties: Map<String, String> = HashMap()

    constructor(scriptLocation: String, clusterLifecycleEvent: ClusterLifecycleEvent,
                executionType: ExecutionType, properties: Map<String, String>) : this(scriptLocation, clusterLifecycleEvent, executionType) {
        this.properties = properties
    }
}
