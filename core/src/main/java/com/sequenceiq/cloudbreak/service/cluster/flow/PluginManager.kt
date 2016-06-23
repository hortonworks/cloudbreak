package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

interface PluginManager {

    fun prepareKeyValues(clientConfig: HttpClientConfig, keyValues: Map<String, String>)

    fun installPlugins(clientConfig: HttpClientConfig, plugins: Map<String, ExecutionType>, hosts: Set<String>,
                       existingHostGroup: Boolean): Map<String, Set<String>>

    fun cleanupPlugins(clientConfig: HttpClientConfig, hosts: Set<String>): Map<String, Set<String>>

    @Throws(CloudbreakSecuritySetupException::class)
    fun waitForEventFinish(stack: Stack, instanceMetaData: Collection<InstanceMetaData>, eventIds: Map<String, Set<String>>, timeout: Int?)

    @Throws(CloudbreakSecuritySetupException::class)
    fun triggerAndWaitForPlugins(stack: Stack, event: ConsulPluginEvent, timeout: Int?, container: DockerContainer)

    @Throws(CloudbreakSecuritySetupException::class)
    fun triggerAndWaitForPlugins(stack: Stack, event: ConsulPluginEvent, timeout: Int?, container: DockerContainer, payload: List<String>, hosts: Set<String>)
}
