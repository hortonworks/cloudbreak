package com.sequenceiq.cloudbreak.websocket

import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP
import org.junit.Assert.assertEquals
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.Arrays

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.notification.Notification
import com.sequenceiq.cloudbreak.service.notification.NotificationSender

@RunWith(MockitoJUnitRunner::class)
class UptimeNotifierTest {

    @InjectMocks
    private val underTest: UptimeNotifier? = null

    @Mock
    private val clusterRepository: ClusterRepository? = null

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val notificationSender: NotificationSender? = null


    @Test
    fun notificationSendingWhenEverythingWorkFine() {
        doNothing().`when`<NotificationSender>(notificationSender).send(any<Notification>(Notification::class.java))
        val clusters = TestUtil.generateCluster(1)

        `when`(clusterRepository!!.findAll()).thenReturn(Arrays.asList(clusters[0]))
        val stack1 = TestUtil.stack()
        `when`(stackRepository!!.findStackForCluster(anyLong())).thenReturn(stack1)

        underTest!!.sendUptime()

        val argument1 = ArgumentCaptor.forClass<Notification>(Notification::class.java)
        verify<NotificationSender>(notificationSender).send(argument1.capture())
        assertEquals(GCP, argument1.value.cloud)
        assertEquals("null", argument1.value.blueprintName)
        assertEquals(null, argument1.value.blueprintId)

        verify<NotificationSender>(notificationSender, times(1)).send(any<Notification>(Notification::class.java))
    }

    @Test
    fun notificationSendingWhenBlueprintNotNullEverythingWorkFine() {
        doNothing().`when`<NotificationSender>(notificationSender).send(any<Notification>(Notification::class.java))
        val clusters = TestUtil.generateCluster(1)

        `when`(clusterRepository!!.findAll()).thenReturn(Arrays.asList(clusters[0]))

        val stack2 = TestUtil.stack()
        stack2.cluster = clusters[0]
        `when`(stackRepository!!.findStackForCluster(anyLong())).thenReturn(stack2)

        underTest!!.sendUptime()

        val argument2 = ArgumentCaptor.forClass<Notification>(Notification::class.java)
        verify<NotificationSender>(notificationSender).send(argument2.capture())
        assertEquals(GCP, argument2.value.cloud)
        assertEquals("multi-node-yarn", argument2.value.blueprintName)
        assertEquals(java.lang.Long.valueOf(1), argument2.value.blueprintId)

        verify<NotificationSender>(notificationSender, times(1)).send(any<Notification>(Notification::class.java))
    }

    @Test
    fun notificationSendingWhenCredentialNullEverythingWorkFine() {
        doNothing().`when`<NotificationSender>(notificationSender).send(any<Notification>(Notification::class.java))
        val clusters = TestUtil.generateCluster(1)

        `when`(clusterRepository!!.findAll()).thenReturn(Arrays.asList(clusters[0]))

        val stack2 = TestUtil.stack()
        stack2.cluster = clusters[0]
        stack2.credential = null
        `when`(stackRepository!!.findStackForCluster(anyLong())).thenReturn(stack2)

        underTest!!.sendUptime()

        val argument2 = ArgumentCaptor.forClass<Notification>(Notification::class.java)
        verify<NotificationSender>(notificationSender).send(argument2.capture())
        assertEquals("null", argument2.value.cloud)
        assertEquals("multi-node-yarn", argument2.value.blueprintName)
        assertEquals(java.lang.Long.valueOf(1), argument2.value.blueprintId)

        verify<NotificationSender>(notificationSender, times(1)).send(any<Notification>(Notification::class.java))
    }
}