package com.sequenceiq.cloudbreak.repository

import org.junit.Assert.assertEquals
import org.mockito.AdditionalAnswers.returnsFirstArg
import org.mockito.Matchers.any
import org.mockito.Matchers.anyList
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter
import com.sequenceiq.cloudbreak.domain.Resource
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService

@RunWith(MockitoJUnitRunner::class)
class StackUpdaterTest {

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val cloudbreakEventService: CloudbreakEventService? = null

    @Mock
    private val resourceRepository: ResourceRepository? = null

    @Mock
    private val statusToPollGroupConverter: StatusToPollGroupConverter? = null

    @InjectMocks
    private val underTest: StackUpdater? = null

    @Test
    fun updateStackStatusWithoutStatusReasonThenNoNotificationSentOnWebsocket() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findById(anyLong())).thenReturn(stack)
        `when`(stackRepository.save(any<Stack>(Stack::class.java))).thenReturn(stack)
        doNothing().`when`<CloudbreakEventService>(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString())
        `when`(statusToPollGroupConverter!!.convert(Status.DELETE_COMPLETED)).thenReturn(PollGroup.POLLABLE)

        val newStack = underTest!!.updateStackStatus(1L, Status.DELETE_COMPLETED)
        assertEquals(Status.DELETE_COMPLETED, newStack.status)
        assertEquals("", newStack.statusReason)
        verify<CloudbreakEventService>(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), anyString())
    }

    @Test
    fun updateStackStatusAndReasonThenNotificationSentOnWebsocket() {
        val stack = TestUtil.stack()

        `when`(stackRepository!!.findById(anyLong())).thenReturn(stack)
        `when`(stackRepository.save(any<Stack>(Stack::class.java))).thenReturn(stack)
        doNothing().`when`<CloudbreakEventService>(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString())
        `when`(statusToPollGroupConverter!!.convert(Status.DELETE_COMPLETED)).thenReturn(PollGroup.POLLABLE)

        val newStack = underTest!!.updateStackStatus(1L, Status.DELETE_COMPLETED, "test")
        assertEquals(Status.DELETE_COMPLETED, newStack.status)
        assertEquals("test", newStack.statusReason)
    }

    @Test
    fun addStackResourcesWithThreeNewResource() {
        val stack = TestUtil.stack()
        val resources = TestUtil.generateGcpResources(5)

        `when`(stackRepository!!.findById(anyLong())).thenReturn(stack)
        `when`(stackRepository.findOneWithLists(anyLong())).thenReturn(stack)
        `when`(stackRepository.save(any<Stack>(Stack::class.java))).then(returnsFirstArg<Any>())
        `when`<Iterable>(resourceRepository!!.save(anyList())).then(returnsFirstArg<Any>())

        val newStack = underTest!!.addStackResources(1L, resources)
        assertEquals(5, newStack.resources.size.toLong())
        verify(resourceRepository, times(1)).save(anyList())
        verify(stackRepository, times(1)).save(any<Stack>(Stack::class.java))
    }

}