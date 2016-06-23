package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

class AmbariOperations : StackContext {

    val ambariClient: AmbariClient
    val ambariOperationType: AmbariOperationType
    val requests: Map<String, Int>
    val requestContext: String
    val requestStatus: String

    constructor(stack: Stack, ambariClient: AmbariClient, requests: Map<String, Int>, ambariOperationType: AmbariOperationType) : super(stack) {
        this.ambariClient = ambariClient
        this.requests = requests
        this.ambariOperationType = ambariOperationType
    }

    constructor(stack: Stack, ambariClient: AmbariClient, requestContext: String, requestStatus: String, ambariOperationType: AmbariOperationType) : super(stack) {
        this.ambariClient = ambariClient
        this.requestContext = requestContext
        this.requestStatus = requestStatus
        this.ambariOperationType = ambariOperationType
    }
}
