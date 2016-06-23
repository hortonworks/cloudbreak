package com.sequenceiq.cloudbreak.core.cluster

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.core.CloudbreakException
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.stack.StackService

@Service
class AmbariClusterCreationService {
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null

    @Throws(CloudbreakException::class)
    fun startAmbari(stackId: Long?) {
        val stack = stackService!!.getById(stackId)
        ambariClusterConnector!!.waitForAmbariServer(stack)
        ambariClusterConnector.changeOriginalAmbariCredentials(stack)
    }

    fun buildAmbariCluster(stackId: Long?) {
        val stack = stackService!!.getById(stackId)
        ambariClusterConnector!!.buildAmbariCluster(stack)
    }
}
