package com.sequenceiq.cloudbreak.service.cluster.flow.status

import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.service.TlsSecurityService
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

class AmbariClusterStatusUpdaterTest {

    @InjectMocks
    private var underTest: AmbariClusterStatusUpdater? = null
    @Mock
    private val clusterService: ClusterService? = null
    @Mock
    private val ambariClientProvider: AmbariClientProvider? = null
    @Mock
    private val cloudbreakEventService: CloudbreakEventService? = null
    @Mock
    private val clusterStatusFactory: AmbariClusterStatusFactory? = null
    @Mock
    private val tlsSecurityService: TlsSecurityService? = null
    @Mock
    private val ambariClient: AmbariClient? = null
    @Mock
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    @Before
    fun setUp() {
        underTest = AmbariClusterStatusUpdater()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(CloudbreakSecuritySetupException::class)
    fun testUpdateClusterStatusShouldUpdateStackStatusWhenStackStatusChanged() {
        // GIVEN
        val stack = createStack(Status.AVAILABLE, Status.AVAILABLE)
        BDDMockito.given(ambariClientProvider!!.getAmbariClient(BDDMockito.any<HttpClientConfig>(HttpClientConfig::class.java), BDDMockito.anyInt(), BDDMockito.any<String>(String::class.java),
                BDDMockito.any<String>(String::class.java))).willReturn(ambariClient)
        BDDMockito.given(clusterStatusFactory!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.INSTALLED)
        // WHEN
        underTest!!.updateClusterStatus(stack, stack.cluster)
        // THEN
        BDDMockito.verify<ClusterService>(clusterService, BDDMockito.times(1)).updateClusterStatusByStackId(stack.id, Status.STOPPED)
    }

    @Test
    @Throws(CloudbreakSecuritySetupException::class)
    fun testUpdateClusterStatusShouldOnlyNotifyWhenStackStatusNotChanged() {
        // GIVEN
        val stack = createStack(Status.AVAILABLE, Status.AVAILABLE)
        BDDMockito.given(ambariClientProvider!!.getAmbariClient(BDDMockito.any<HttpClientConfig>(HttpClientConfig::class.java), BDDMockito.anyInt(), BDDMockito.any<String>(String::class.java),
                BDDMockito.any<String>(String::class.java))).willReturn(ambariClient)
        BDDMockito.given(clusterStatusFactory!!.createClusterStatus(ambariClient, TEST_BLUEPRINT)).willReturn(ClusterStatus.STARTED)
        // WHEN
        underTest!!.updateClusterStatus(stack, stack.cluster)
        // THEN
        BDDMockito.verify<ClusterService>(clusterService, BDDMockito.times(0)).updateClusterStatusByStackId(BDDMockito.any<Long>(Long::class.java), BDDMockito.any<Status>(Status::class.java))
    }

    private fun createStack(stackStatus: Status): Stack {
        val stack = Stack()
        stack.id = TEST_STACK_ID
        stack.status = stackStatus
        return stack
    }

    private fun createStack(stackStatus: Status, clusterStatus: Status): Stack {
        val stack = createStack(stackStatus)
        val cluster = Cluster()
        cluster.ambariIp = "10.0.0.1"
        cluster.id = TEST_CLUSTER_ID
        cluster.status = clusterStatus
        val blueprint = Blueprint()
        blueprint.blueprintName = TEST_BLUEPRINT
        cluster.blueprint = blueprint
        stack.cluster = cluster
        return stack
    }

    companion object {
        private val TEST_STACK_ID = 0L
        private val TEST_CLUSTER_ID = 0L
        private val TEST_BLUEPRINT = "blueprint"
        private val TEST_REASON = "Reason"
    }
}
