package com.sequenceiq.cloudbreak.service.cluster.flow

import org.mockito.Matchers.any
import org.mockito.Matchers.anyBoolean
import org.mockito.Matchers.anyCollection
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyMap
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.ambari.client.InvalidHostGroupHostAssociation
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.StatusCheckerTask
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException
import com.sequenceiq.cloudbreak.service.cluster.HadoopConfigurationService
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

import groovyx.net.http.HttpResponseException
import reactor.bus.EventBus

@RunWith(MockitoJUnitRunner::class)
class AmbariClusterConnectorTest {

    @Mock
    private val pluginManager: PluginManager? = null

    @Mock
    private val tlsSecurityService: TlsSecurityService? = null

    @Mock
    private val reactor: EventBus? = null

    @Mock
    private val ambariClient: AmbariClient? = null

    @Mock
    private val httpClientConfig: HttpClientConfig? = null

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val ambariClientProvider: AmbariClientProvider? = null

    @Mock
    private val hadoopConfigurationService: HadoopConfigurationService? = null

    @Mock
    private val hostsPollingService: PollingService<AmbariHostsCheckerContext>? = null

    @Mock
    private val ambariHostsStatusCheckerTask: AmbariHostsStatusCheckerTask? = null

    @Mock
    private val hostGroupRepository: HostGroupRepository? = null

    @Mock
    private val ambariOperationService: AmbariOperationService? = null

    @Mock
    private val clusterRepository: ClusterRepository? = null

    @Mock
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null

    @Mock
    private val hostMetadataRepository: HostMetadataRepository? = null

    @Mock
    private val hostGroup: HostGroup? = null

    @Mock
    private val messagesService: CloudbreakMessagesService? = null

    @Mock
    private val blueprintProcessor: BlueprintProcessor? = null

    @InjectMocks
    @Spy
    private val underTest = AmbariClusterConnector()

    private var stack: Stack? = null

    private var cluster: Cluster? = null

    private var blueprint: Blueprint? = null

    @Before
    @Throws(CloudbreakSecuritySetupException::class, HttpResponseException::class, InvalidHostGroupHostAssociation::class)
    fun setUp() {
        stack = TestUtil.stack()
        blueprint = TestUtil.blueprint()
        cluster = TestUtil.cluster(blueprint, stack, 1L)
        stack!!.cluster = cluster
        cluster!!.hostGroups = HashSet<HostGroup>()
        cluster!!.configStrategy = ConfigStrategy.NEVER_APPLY
        `when`(tlsSecurityService!!.buildTLSClientConfig(anyLong(), anyString())).thenReturn(httpClientConfig)
        `when`(ambariClient!!.extendBlueprintGlobalConfiguration(anyString(), anyMap())).thenReturn("")
        `when`(hostMetadataRepository!!.findHostsInCluster(anyLong())).thenReturn(HashSet<HostMetadata>())
        `when`(ambariClient.extendBlueprintHostGroupConfiguration(anyString(), anyMap())).thenReturn(blueprint!!.blueprintText)
        `when`(ambariClient.addBlueprint(anyString())).thenReturn("")
        `when`(hadoopConfigurationService!!.getHostGroupConfiguration(any<Cluster>(Cluster::class.java))).thenReturn(HashMap<String, Map<String, Map<String, String>>>())
        `when`(ambariClientProvider!!.getAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), anyString(), anyString())).thenReturn(ambariClient)
        `when`(ambariClientProvider.getDefaultAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt())).thenReturn(ambariClient)
        `when`(hostsPollingService!!.pollWithTimeoutSingleFailure(any<AmbariHostsStatusCheckerTask>(AmbariHostsStatusCheckerTask::class.java), any<AmbariHostsCheckerContext>(AmbariHostsCheckerContext::class.java), anyInt(),
                anyInt())).thenReturn(PollingResult.SUCCESS)
        `when`(hostGroupRepository!!.findHostGroupsInCluster(anyLong())).thenReturn(cluster!!.hostGroups)
        `when`(ambariOperationService!!.waitForOperations(any<Stack>(Stack::class.java), any<AmbariClient>(AmbariClient::class.java), anyMap(), any<AmbariOperationType>(AmbariOperationType::class.java))).thenReturn(PollingResult.SUCCESS)
        `when`<PollingResult>(ambariOperationService.waitForOperations(any<Stack>(Stack::class.java), any<AmbariClient>(AmbariClient::class.java), any<StatusCheckerTask>(StatusCheckerTask<Any>::class.java), anyMap(),
                any<AmbariOperationType>(AmbariOperationType::class.java))).thenReturn(PollingResult.SUCCESS)
        `when`(clusterRepository!!.save(any<Cluster>(Cluster::class.java))).thenReturn(cluster)
        `when`<Iterable>(instanceMetadataRepository!!.save(anyCollection())).thenReturn(stack!!.runningInstanceMetaData)
        `when`(ambariClient.recommendAssignments(anyString())).thenReturn(createStringListMap())
        `when`(ambariClient.deleteUser(anyString())).thenReturn("")
        `when`(ambariClient.createUser(anyString(), anyString(), anyBoolean())).thenReturn("")
        `when`(ambariClient.changePassword(anyString(), anyString(), anyString(), anyBoolean())).thenReturn("")
        `when`(ambariClientProvider.getSecureAmbariClient(any<HttpClientConfig>(HttpClientConfig::class.java), anyInt(), any<Cluster>(Cluster::class.java))).thenReturn(ambariClient)
        `when`(stackRepository!!.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(clusterRepository.findOneWithLists(anyLong())).thenReturn(cluster)
    }

    @Test(expected = AmbariOperationFailedException::class)
    @Throws(Exception::class)
    fun testInstallAmbariWhenExceptionOccursShouldInstallationFailed() {
        doThrow(IllegalArgumentException()).`when`<AmbariClient>(ambariClient).createCluster(anyString(), anyString(), anyMap(), anyString(), anyString())
        underTest.buildAmbariCluster(stack)
    }

    @Test(expected = AmbariOperationFailedException::class)
    @Throws(Exception::class)
    fun testInstallAmbariWhenReachedMaxPollingEventsShouldInstallationFailed() {
        `when`(ambariOperationService!!.waitForOperations(any<Stack>(Stack::class.java), any<AmbariClient>(AmbariClient::class.java), anyMap(), any<AmbariOperationType>(AmbariOperationType::class.java))).thenReturn(PollingResult.TIMEOUT)
        underTest.buildAmbariCluster(stack)
    }

    @Test
    @Throws(Exception::class)
    fun testChangeAmbariCredentialsWhenUserIsTheSameThenModifyUser() {
        underTest.credentialChangeAmbariCluster(stack!!.id, "admin", "admin1")
        verify<AmbariClient>(ambariClient, times(1)).changePassword(anyString(), anyString(), anyString(), anyBoolean())
        verify<AmbariClient>(ambariClient, times(0)).deleteUser(anyString())
        verify<AmbariClient>(ambariClient, times(0)).createUser(anyString(), anyString(), anyBoolean())
    }

    @Test
    @Throws(Exception::class)
    fun testChangeAmbariCredentialsWhenUserDifferentThanExistThenCreateNewUserDeleteOldOne() {
        underTest.credentialChangeAmbariCluster(stack!!.id, "admin123", "admin1")
        verify<AmbariClient>(ambariClient, times(0)).changePassword(anyString(), anyString(), anyString(), anyBoolean())
        verify<AmbariClient>(ambariClient, times(1)).deleteUser(anyString())
        verify<AmbariClient>(ambariClient, times(1)).createUser(anyString(), anyString(), anyBoolean())
    }

    private fun createStringListMap(): Map<String, List<String>> {
        val stringListMap = HashMap<String, List<String>>()
        stringListMap.put("a1", Arrays.asList("assignment1", "assignment2"))
        return stringListMap
    }
}
