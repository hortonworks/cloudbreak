package com.sequenceiq.cloudbreak.core.bootstrap.service

import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anySet
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.HashSet

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.stubbing.Answer

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.common.type.ResourceType
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.ResourceRepository
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter

@RunWith(MockitoJUnitRunner::class)
class ClusterBootstrapperErrorHandlerTest {

    @Mock
    private val resourceRepository: ResourceRepository? = null

    @Mock
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Mock
    private val instanceGroupRepository: InstanceGroupRepository? = null

    @Mock
    private val hostMetadataRepository: HostMetadataRepository? = null

    //private CloudPlatformResolver platformResolver;

    @Mock
    private val eventService: CloudbreakEventService? = null

    @Mock
    private val orchestrator: ContainerOrchestrator? = null

    //@Mock
    //private ServiceProviderMetadataAdapter metadataSetup;

    @Mock
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    @Mock
    private val connector: ServiceProviderConnectorAdapter? = null

    @Mock
    private val metadata: ServiceProviderMetadataAdapter? = null


    @InjectMocks
    private val underTest: ClusterBootstrapperErrorHandler? = null

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(CloudbreakOrchestratorFailedException::class)
    fun clusterBootstrapErrorHandlerWhenNodeCountLessThanOneAfterTheRollbackThenClusterProvisionFailed() {
        val stack = TestUtil.stack()

        doNothing().`when`<CloudbreakEventService>(eventService).fireCloudbreakEvent(anyLong(), anyString(), anyString())
        `when`(orchestrator!!.getAvailableNodes(any<GatewayConfig>(GatewayConfig::class.java), anySet())).thenReturn(ArrayList<String>())
        `when`(instanceGroupRepository!!.save(any<InstanceGroup>(InstanceGroup::class.java))).then(returnsFirstArg<Any>())
        `when`(instanceMetaDataRepository!!.save(any<InstanceMetaData>(InstanceMetaData::class.java))).then(returnsFirstArg<Any>())
        `when`(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer(Answer<com.sequenceiq.cloudbreak.domain.InstanceMetaData> { invocation ->
            val args = invocation.arguments
            val ip = args[1] as String
            for (instanceMetaData in stack.runningInstanceMetaData) {
                if (instanceMetaData.privateIp == ip) {
                    return@Answer instanceMetaData
                }
            }
            null
        })
        `when`(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer(Answer<com.sequenceiq.cloudbreak.domain.InstanceGroup> { invocation ->
            val args = invocation.arguments
            val name = args[1] as String
            for (instanceMetaData in stack.runningInstanceMetaData) {
                if (instanceMetaData.instanceGroup.groupName == name) {
                    return@Answer instanceMetaData.instanceGroup
                }
            }
            null
        })

        underTest!!.terminateFailedNodes(null, orchestrator, TestUtil.stack(), GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"), prepareNodes(stack))
    }

    @Test
    @Throws(CloudbreakOrchestratorFailedException::class)
    fun clusterBootstrapErrorHandlerWhenNodeCountHigherThanZeroAfterTheRollbackThenClusterProvisionFailed() {
        val stack = TestUtil.stack()

        doNothing().`when`<CloudbreakEventService>(eventService).fireCloudbreakEvent(anyLong(), anyString(), anyString())
        `when`(orchestrator!!.getAvailableNodes(any<GatewayConfig>(GatewayConfig::class.java), anySet())).thenReturn(ArrayList<String>())
        `when`(instanceGroupRepository!!.save(any<InstanceGroup>(InstanceGroup::class.java))).then(returnsFirstArg<Any>())
        `when`(instanceMetaDataRepository!!.save(any<InstanceMetaData>(InstanceMetaData::class.java))).then(returnsFirstArg<Any>())
        doNothing().`when`<ResourceRepository>(resourceRepository).delete(anyLong())
        `when`(resourceRepository!!.findByStackIdAndNameAndType(anyLong(), anyString(), any<ResourceType>(ResourceType::class.java))).thenReturn(Resource())
        `when`(connector!!.removeInstances(any<Stack>(Stack::class.java), anySet(), anyString())).thenReturn(HashSet<String>())
        `when`(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenAnswer(Answer<com.sequenceiq.cloudbreak.domain.InstanceMetaData> { invocation ->
            val args = invocation.arguments
            val ip = args[1] as String
            for (instanceMetaData in stack.runningInstanceMetaData) {
                if (instanceMetaData.privateIp == ip) {
                    return@Answer instanceMetaData
                }
            }
            null
        })
        `when`(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenAnswer(Answer<com.sequenceiq.cloudbreak.domain.InstanceGroup> { invocation ->
            val args = invocation.arguments
            val name = args[1] as String
            for (instanceMetaData in stack.runningInstanceMetaData) {
                if (instanceMetaData.instanceGroup.groupName == name) {
                    val instanceGroup = instanceMetaData.instanceGroup
                    instanceGroup.nodeCount = 2
                    return@Answer instanceGroup
                }
            }
            null
        })
        underTest!!.terminateFailedNodes(null, orchestrator, TestUtil.stack(), GatewayConfig("10.0.0.1", "10.0.0.1", 8443, "/cert/1"), prepareNodes(stack))

        verify<CloudbreakEventService>(eventService, times(4)).fireCloudbreakEvent(anyLong(), anyString(), anyString())
        verify(instanceGroupRepository, times(3)).save(any<InstanceGroup>(InstanceGroup::class.java))
        verify(instanceMetaDataRepository, times(3)).save(any<InstanceMetaData>(InstanceMetaData::class.java))
        verify(connector, times(3)).removeInstances(any<Stack>(Stack::class.java), anySet(), anyString())
        verify(resourceRepository, times(3)).findByStackIdAndNameAndType(anyLong(), anyString(), any<ResourceType>(ResourceType::class.java))
        verify(resourceRepository, times(3)).delete(anyLong())
        verify(instanceGroupRepository, times(3)).findOneByGroupNameInStack(anyLong(), anyString())

    }

    private fun prepareNodes(stack: Stack): Set<Node> {
        val nodes = HashSet<Node>()
        for (instanceMetaData in stack.runningInstanceMetaData) {
            nodes.add(Node(instanceMetaData.privateIp, instanceMetaData.publicIpWrapper))
        }
        return nodes
    }

}