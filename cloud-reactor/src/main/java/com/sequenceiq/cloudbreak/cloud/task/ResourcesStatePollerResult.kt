package com.sequenceiq.cloudbreak.cloud.task

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

class ResourcesStatePollerResult {

    var cloudContext: CloudContext? = null
    var status: ResourceStatus? = null
    var statusReason: String? = null
    private var results: MutableList<CloudResourceStatus>? = null

    constructor(cloudContext: CloudContext) {
        this.cloudContext = cloudContext
        this.results = ArrayList<CloudResourceStatus>()
    }

    constructor(cloudContext: CloudContext, status: ResourceStatus, statusReason: String, results: MutableList<CloudResourceStatus>) {
        this.cloudContext = cloudContext
        this.status = status
        this.statusReason = statusReason
        this.results = results
    }

    fun getResults(): List<CloudResourceStatus> {
        return results
    }

    fun addResults(results: List<CloudResourceStatus>) {
        this.results!!.addAll(results)
    }

    override fun toString(): String {
        return "ResourcesStatePollerResult{"
        +"cloudContext=" + cloudContext
        +", status=" + status
        +", statusReason='" + statusReason + '\''
        +", results=" + results
        +'}'
    }
}
