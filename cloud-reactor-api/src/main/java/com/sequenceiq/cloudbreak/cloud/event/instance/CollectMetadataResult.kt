package com.sequenceiq.cloudbreak.cloud.event.instance

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus

class CollectMetadataResult : CloudPlatformResult<CollectMetadataRequest> {
    val results: List<CloudVmMetaDataStatus>

    constructor(request: CollectMetadataRequest, results: List<CloudVmMetaDataStatus>) : super(request) {
        this.results = results
    }

    constructor(errorDetails: Exception, request: CollectMetadataRequest) : super("", errorDetails, request) {
    }

    override fun toString(): String {
        val sb = StringBuilder("CollectMetadataResult{")
        sb.append("cloudContext=").append(request.cloudContext)
        sb.append(", results=").append(results)
        sb.append(", exception=").append(errorDetails)
        sb.append('}')
        return sb.toString()
    }
}
