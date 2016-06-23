package com.sequenceiq.cloudbreak.service.stack

import com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE
import com.sequenceiq.cloudbreak.api.model.Status.START_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.STOPPED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED
import com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS
import com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED
import com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyString
import org.mockito.Matchers.eq
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Credential
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.StackUpdater
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService

@RunWith(MockitoJUnitRunner::class)
class DefaultStackHostServiceTypeTest {

    @InjectMocks
    private val underTest: StackService? = null

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val stackUpdater: StackUpdater? = null

    @Mock
    private val clusterRepository: ClusterRepository? = null

    @Mock
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    @Mock
    private val flowManager: ReactorFlowManager? = null

    @Mock
    private val blueprintValidator: BlueprintValidator? = null

    @Mock
    private val cloudbreakMessagesService: CloudbreakMessagesService? = null

    @Mock
    private val eventService: CloudbreakEventService? = null

    @Before
    fun before() {
        doNothing().`when`<ReactorFlowManager>(flowManager).triggerStackStop(anyObject<Long>())
        doNothing().`when`<ReactorFlowManager>(flowManager).triggerStackStart(anyObject<Long>())
    }

    @Test
    fun updateStatusTestStopWhenClusterStoppedThenStackStopTriggered() {
        val stack = stack(AVAILABLE, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStop(anyObject<Long>())
    }

    @Test
    fun updateStatusTestStopWhenClusterInStopInProgressThenTriggeredStackStopRequested() {
        val stack = stack(AVAILABLE, STOP_IN_PROGRESS)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
        verify<CloudbreakEventService>(eventService, times(1)).fireCloudbreakEvent(eq(1L), eq(STOP_REQUESTED.name), anyString())
    }

    @Test
    fun updateStatusTestStopWhenClusterInStoppedAndStackAvailableThenTriggerStackStop() {
        val stack = stack(AVAILABLE, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStop(anyObject<Long>())
    }

    @Test
    fun updateStatusTestStopWhenClusterInStoppedAndStackStopFailedThenTriggerStackStop() {
        val stack = stack(STOP_FAILED, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStop(anyObject<Long>())
    }

    @Test(expected = BadRequestException::class)
    fun updateStatusTestStopWhenClusterInStoppedAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        val stack = stack(UPDATE_IN_PROGRESS, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStop(anyObject<Long>())
    }


    @Test(expected = BadRequestException::class)
    fun updateStatusTestStopWhenClusterAndStackAvailableThenBadRequestExceptionDropping() {
        val stack = stack(AVAILABLE, AVAILABLE)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
    }

    @Test
    fun updateStatusTestStartWhenStackStoppedThenStackStartTriggered() {
        val stack = stack(STOPPED, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STARTED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStart(anyObject<Long>())
    }

    @Test(expected = BadRequestException::class)
    fun updateStatusTestStartWhenClusterInStoppedAndStackStopFailedThenBadRequestExceptionDropping() {
        val stack = stack(UPDATE_IN_PROGRESS, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STARTED)
    }

    @Test
    fun updateStatusTestStartWhenClusterInStoppedAndStackStartFailedThenTriggerStackStart() {
        val stack = stack(START_FAILED, STOPPED)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STARTED)
        verify<ReactorFlowManager>(flowManager, times(1)).triggerStackStart(anyObject<Long>())
    }

    @Test(expected = BadRequestException::class)
    fun updateStatusTestStartWhenClusterAndStackUpdateInProgressThenBadRequestExceptionDropping() {
        val stack = stack(UPDATE_IN_PROGRESS, UPDATE_IN_PROGRESS)
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STARTED)
    }

    @Test(expected = BadRequestException::class)
    fun updateStatusTestStopWhenClusterAndStackAvailableAndEphemeralThenBadRequestExceptionDropping() {
        val stack = TestUtil.setEphemeral(TestUtil.stack(AVAILABLE, TestUtil.awsCredential()))
        given(stackRepository!!.findOneWithLists(anyLong())).willReturn(stack)
        given(clusterRepository!!.findOneWithLists(anyLong())).willReturn(stack.cluster)
        given(stackUpdater!!.updateStackStatus(anyLong(), any<Status>(Status::class.java))).willReturn(stack)
        underTest!!.updateStatus(1L, StatusRequest.STOPPED)
    }

    private fun stack(stackStatus: Status, clusterStatus: Status): Stack {
        val gcpCredential = Credential()
        val stack = Stack()
        stack.status = stackStatus
        stack.credential = gcpCredential
        stack.id = 1L
        val cluster = Cluster()
        cluster.status = clusterStatus
        cluster.id = 1L
        stack.cluster = cluster
        return stack
    }

}