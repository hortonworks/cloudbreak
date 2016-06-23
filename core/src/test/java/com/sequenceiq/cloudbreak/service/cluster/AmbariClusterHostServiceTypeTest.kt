package com.sequenceiq.cloudbreak.service.cluster

import java.util.Arrays.asList
import java.util.Collections.singletonMap
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.net.ConnectException
import java.util.HashMap
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.stack.StackService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

import groovyx.net.http.HttpResponseException

@RunWith(MockitoJUnitRunner::class)
class AmbariClusterHostServiceTypeTest {

    @Mock
    private val stackService: StackService? = null

    @Mock
    private val clusterRepository: ClusterRepository? = null

    @Mock
    private val ambariClient: AmbariClient? = null

    @Mock
    private val ambariClientProvider: AmbariClientProvider? = null

    @Mock
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null

    @Mock
    private val tlsSecurityService: TlsSecurityService? = null

    @Mock
    private val flowManager: ReactorFlowManager? = null

    @Mock
    private val hostGroupService: HostGroupService? = null

    @Mock
    private val statusToPollGroupConverter: StatusToPollGroupConverter? = null

    @InjectMocks
    @Spy
    private val underTest = AmbariClusterService()

    private var stack: Stack? = null

    private var clusterRequest: ClusterRequest? = null

    private var clusterResponse: ClusterResponse? = null

    private var cluster: Cluster? = null

    @Before
    @Throws(CloudbreakSecuritySetupException::class)
    fun setUp() {
        stack = TestUtil.stack()
        cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack!!.cluster = cluster
        clusterRequest = ClusterRequest()
        clusterResponse = ClusterResponse()
        `when`(stackService!!.get(anyLong())).thenReturn(stack)
        `when`(stackService.getById(anyLong())).thenReturn(stack)
        `when`(stackService.findLazy(anyLong())).thenReturn(stack)
        `when`(clusterRepository!!.save(any<Cluster>(Cluster::class.java))).thenReturn(cluster)
        given(tlsSecurityService!!.buildTLSClientConfig(anyLong(), anyString())).willReturn(HttpClientConfig("", 8443, "/tmp"))
    }

    @Test(expected = BadRequestException::class)
    fun testStopWhenAwsHasEphemeralVolume() {
        cluster = TestUtil.cluster(TestUtil.blueprint(), TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential()), 1L)
        cluster!!.stack.setCloudPlatform("AWS")
        stack = TestUtil.setEphemeral(cluster!!.stack)
        cluster!!.status = Status.AVAILABLE
        cluster!!.stack = stack
        stack!!.cluster = cluster

        `when`(stackService!!.get(anyLong())).thenReturn(stack)
        `when`(stackService.getById(anyLong())).thenReturn(stack)

        underTest.updateStatus(1L, StatusRequest.STOPPED)
    }

    @Test(expected = BadRequestException::class)
    @Throws(HttpResponseException::class)
    fun testRetrieveClusterJsonWhenClusterJsonIsNull() {
        // GIVEN
        doReturn(ambariClient).`when`<AmbariClientProvider>(ambariClientProvider).getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))
        given(ambariClient!!.clusterAsJson).willReturn(null)
        // WHEN
        underTest.getClusterJson("123.12.3.4", 1L)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testUpdateHostsDoesntAcceptZeroScalingAdjustments() {
        // GIVEN
        val hga1 = HostGroupAdjustmentJson()
        hga1.hostGroup = "slave_1"
        hga1.scalingAdjustment = 0
        // WHEN
        underTest.updateHosts(stack!!.id, hga1)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testUpdateHostsDoesntAcceptScalingAdjustmentsWithDifferentSigns() {
        // GIVEN
        val hga1 = HostGroupAdjustmentJson()
        hga1.hostGroup = "slave_1"
        hga1.scalingAdjustment = -2
        // WHEN
        underTest.updateHosts(stack!!.id, hga1)
    }

    @Test
    @Throws(ConnectException::class, CloudbreakSecuritySetupException::class)
    fun testUpdateHostsForDownscaleFilterAllHosts() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -1
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val hostsMetaData = HashSet<HostMetadata>()
        hostsMetaData.addAll(asList(metadata1, metadata2, metadata3))
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)
    }

    @Test
    @Throws(ConnectException::class, CloudbreakSecuritySetupException::class)
    fun testUpdateHostsForDownscaleCannotGoBelowReplication() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -1
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val hostsMetaData = HashSet<HostMetadata>()
        val hostsMetadataList = asList(metadata1, metadata2, metadata3)
        hostsMetaData.addAll(hostsMetadataList)
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)

        verify<ReactorFlowManager>(flowManager, times(1)).triggerClusterDownscale(stack!!.id, json)
    }

    @Test
    @Throws(ConnectException::class, CloudbreakSecuritySetupException::class)
    fun testUpdateHostsForDownscaleFilterOneHost() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -1
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData1 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData2 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData3 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata4 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData4 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val hostsMetaData = HashSet(asList(metadata1, metadata2, metadata3, metadata4))
        val hostsMetadataList = asList(metadata2, metadata3, metadata4)
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        val dfsSpace = HashMap<String, Map<Long, Long>>()
        dfsSpace.put("node2", singletonMap<Long, Long>(85_000L, 15_000L))
        dfsSpace.put("node1", singletonMap<Long, Long>(90_000L, 10_000L))
        dfsSpace.put("node3", singletonMap<Long, Long>(80_000L, 20_000L))
        dfsSpace.put("node4", singletonMap<Long, Long>(80_000L, 11_000L))
        `when`(metadata1.hostName).thenReturn("node1")
        `when`(metadata2.hostName).thenReturn("node2")
        `when`(metadata3.hostName).thenReturn("node3")
        `when`(metadata4.hostName).thenReturn("node4")
        `when`(instanceMetaData1.ambariServer).thenReturn(false)
        `when`(instanceMetaData2.ambariServer).thenReturn(false)
        `when`(instanceMetaData3.ambariServer).thenReturn(false)
        `when`(instanceMetaData4.ambariServer).thenReturn(false)
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(ambariClient.getBlueprintMap(cluster!!.blueprint.blueprintName)).thenReturn(singletonMap("slave_1", asList("DATANODE")))
        `when`(ambariClient.dfsSpace).thenReturn(dfsSpace)
        `when`(instanceMetadataRepository!!.findHostInStack(stack!!.id, "node1")).thenReturn(instanceMetaData1)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node2")).thenReturn(instanceMetaData2)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node3")).thenReturn(instanceMetaData3)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node4")).thenReturn(instanceMetaData4)
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)

        verify<ReactorFlowManager>(flowManager, times(1)).triggerClusterDownscale(stack!!.id, json)
    }

    @Test
    @Throws(ConnectException::class, CloudbreakSecuritySetupException::class)
    fun testUpdateHostsForDownscaleSelectNodesWithLessData() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -1
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData1 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData2 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData3 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val hostsMetaData = HashSet<HostMetadata>()
        val hostsMetadataList = asList(metadata1, metadata2, metadata3)
        hostsMetaData.addAll(hostsMetadataList)
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        val dfsSpace = HashMap<String, Map<Long, Long>>()
        dfsSpace.put("node2", singletonMap<Long, Long>(85_000L, 15_000L))
        dfsSpace.put("node1", singletonMap<Long, Long>(90_000L, 10_000L))
        dfsSpace.put("node3", singletonMap<Long, Long>(80_000L, 20_000L))
        `when`(metadata1.hostName).thenReturn("node1")
        `when`(metadata2.hostName).thenReturn("node2")
        `when`(metadata3.hostName).thenReturn("node3")
        `when`(instanceMetaData1.ambariServer).thenReturn(false)
        `when`(instanceMetaData2.ambariServer).thenReturn(false)
        `when`(instanceMetaData3.ambariServer).thenReturn(false)
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(ambariClient.getBlueprintMap(cluster!!.blueprint.blueprintName)).thenReturn(singletonMap("slave_1", asList("DATANODE")))
        `when`(ambariClient.dfsSpace).thenReturn(dfsSpace)
        `when`(instanceMetadataRepository!!.findHostInStack(stack!!.id, "node1")).thenReturn(instanceMetaData1)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node2")).thenReturn(instanceMetaData2)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node3")).thenReturn(instanceMetaData3)
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)

        verify<ReactorFlowManager>(flowManager, times(1)).triggerClusterDownscale(stack!!.id, json)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateHostsForDownscaleSelectMultipleNodesWithLessData() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -2
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData1 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData2 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData3 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata4 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData4 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val hostsMetaData = HashSet<HostMetadata>()
        val hostsMetadataList = asList(metadata1, metadata2, metadata3, metadata4)
        hostsMetaData.addAll(hostsMetadataList)
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        val dfsSpace = HashMap<String, Map<Long, Long>>()
        dfsSpace.put("node2", singletonMap<Long, Long>(85_000L, 15_000L))
        dfsSpace.put("node1", singletonMap<Long, Long>(90_000L, 10_000L))
        dfsSpace.put("node3", singletonMap<Long, Long>(80_000L, 20_000L))
        dfsSpace.put("node4", singletonMap<Long, Long>(90_000L, 10_000L))
        `when`(metadata1.hostName).thenReturn("node1")
        `when`(metadata2.hostName).thenReturn("node2")
        `when`(metadata3.hostName).thenReturn("node3")
        `when`(metadata3.hostName).thenReturn("node4")
        `when`(instanceMetaData1.ambariServer).thenReturn(false)
        `when`(instanceMetaData2.ambariServer).thenReturn(false)
        `when`(instanceMetaData3.ambariServer).thenReturn(false)
        `when`(instanceMetaData4.ambariServer).thenReturn(false)
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(ambariClient.getBlueprintMap(cluster!!.blueprint.blueprintName)).thenReturn(singletonMap("slave_1", asList("DATANODE")))
        `when`(ambariClient.dfsSpace).thenReturn(dfsSpace)
        `when`(instanceMetadataRepository!!.findHostInStack(stack!!.id, "node1")).thenReturn(instanceMetaData1)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node2")).thenReturn(instanceMetaData2)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node3")).thenReturn(instanceMetaData3)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node4")).thenReturn(instanceMetaData3)
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)

        verify<ReactorFlowManager>(flowManager, times(1)).triggerClusterDownscale(stack!!.id, json)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateHostsForDownscaleWhenRemainingSpaceIsNotEnough() {
        val json = HostGroupAdjustmentJson()
        json.hostGroup = "slave_1"
        json.scalingAdjustment = -1
        val ambariClient = mock<AmbariClient>(AmbariClient::class.java)
        val metadata1 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData1 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata2 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData2 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val metadata3 = mock<HostMetadata>(HostMetadata::class.java)
        val instanceMetaData3 = mock<InstanceMetaData>(InstanceMetaData::class.java)
        val hostsMetaData = HashSet<HostMetadata>()
        val hostsMetadataList = asList(metadata1, metadata2, metadata3)
        hostsMetaData.addAll(hostsMetadataList)
        val hostGroup = HostGroup()
        hostGroup.hostMetadata = hostsMetaData
        hostGroup.name = "slave_1"
        val dfsSpace = HashMap<String, Map<Long, Long>>()
        dfsSpace.put("node2", singletonMap<Long, Long>(5_000L, 15_000L))
        dfsSpace.put("node1", singletonMap<Long, Long>(10_000L, 10_000L))
        dfsSpace.put("node3", singletonMap<Long, Long>(6_000L, 20_000L))
        `when`(metadata1.hostName).thenReturn("node1")
        `when`(metadata2.hostName).thenReturn("node2")
        `when`(metadata3.hostName).thenReturn("node3")
        `when`(instanceMetaData1.ambariServer).thenReturn(false)
        `when`(instanceMetaData2.ambariServer).thenReturn(false)
        `when`(instanceMetaData3.ambariServer).thenReturn(false)
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<String>(String::class.java), any<String>(String::class.java))).thenReturn(ambariClient)
        `when`(ambariClient.getComponentsCategory("multi-node-yarn", "slave_1")).thenReturn(singletonMap("DATANODE", "SLAVE"))
        `when`(ambariClient.getBlueprintMap(cluster!!.blueprint.blueprintName)).thenReturn(singletonMap("slave_1", asList("DATANODE")))
        `when`(ambariClient.dfsSpace).thenReturn(dfsSpace)
        `when`(hostGroupService!!.getByClusterIdAndName(anyLong(), anyString())).thenReturn(hostGroup)
        `when`(instanceMetadataRepository!!.findHostInStack(stack!!.id, "node1")).thenReturn(instanceMetaData1)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node2")).thenReturn(instanceMetaData2)
        `when`(instanceMetadataRepository.findHostInStack(stack!!.id, "node3")).thenReturn(instanceMetaData3)
        `when`(statusToPollGroupConverter!!.convert(Mockito.any<Status>(Status::class.java))).thenReturn(PollGroup.POLLABLE)

        underTest.updateHosts(stack!!.id, json)

        verify<ReactorFlowManager>(flowManager, times(1)).triggerClusterDownscale(stack!!.id, json)
    }
}
