package com.sequenceiq.cloudbreak.service.events

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP
import org.mockito.BDDMockito.given

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.ServiceTestUtils
import com.sequenceiq.cloudbreak.service.notification.NotificationSender

class DefaultCloudbreakEventHostServiceTypeTest {

    @InjectMocks
    private var eventService: DefaultCloudbreakEventService? = null

    @Mock
    private val eventRepository: CloudbreakEventRepository? = null

    @Mock
    private val stackRepository: StackRepository? = null

    @Mock
    private val notificationSender: NotificationSender? = null

    @Captor
    private val captor: ArgumentCaptor<CloudbreakEvent>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        eventService = DefaultCloudbreakEventService()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateAwsStackEvent() {
        //GIVEN
        val template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, AWS)
        val stack = ServiceTestUtils.createStack("John", "Acme", template, null)

        given(stackRepository!!.findById(1L)).willReturn(stack)
        val stackEvent = CloudbreakEvent()
        stackEvent.owner = "John"
        stackEvent.cloud = AWS

        given(eventRepository!!.save(Mockito.any<CloudbreakEvent>(CloudbreakEvent::class.java))).willReturn(stackEvent)
        val instanceGroup = InstanceGroup()
        instanceGroup.groupName = "master"
        instanceGroup.template = template
        val eventData = CloudbreakEventData(1L, "STACK_CREATED", "Stack created")

        //WHEN
        val event = eventService!!.createStackEvent(eventData)

        Assert.assertNotNull(event)
        Assert.assertEquals("The user name is not the expected", "John", event.owner)
        Assert.assertEquals("The cloudprovider is not the expected", AWS, event.cloud)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateAzureStackEvent() {
        //GIVEN
        val template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, GCP)
        val stack = ServiceTestUtils.createStack("John", "Acme", template, null)

        given(stackRepository!!.findById(1L)).willReturn(stack)
        val stackEvent = CloudbreakEvent()
        stackEvent.cloud = "AZURE"
        stackEvent.owner = "John"
        given(eventRepository!!.save(Mockito.any<CloudbreakEvent>(CloudbreakEvent::class.java))).willReturn(stackEvent)
        val instanceGroup = InstanceGroup()
        instanceGroup.groupName = "master"
        instanceGroup.template = template
        val eventData = CloudbreakEventData(1L, "STACK_CREATED", "Stack created")

        //WHEN
        val event = eventService!!.createStackEvent(eventData)

        Assert.assertNotNull(event)
        Assert.assertEquals("The user name is not the expected", "John", event.owner)
        Assert.assertEquals("The cloudprovider is not the expected", "AZURE", event.cloud)
    }

    @Test
    fun testShouldClusterDataBePopulated() {
        //GIVEN
        val template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, GCP)
        val blueprint = ServiceTestUtils.createBlueprint(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT)
        val cluster = ServiceTestUtils.createCluster("John", "Acme", blueprint)
        val stack = ServiceTestUtils.createStack("John", "Acme", template, cluster)

        given(stackRepository!!.findById(1L)).willReturn(stack)
        val stackEvent = CloudbreakEvent()
        given(eventRepository!!.save(Mockito.any<CloudbreakEvent>(CloudbreakEvent::class.java))).willReturn(stackEvent)
        val instanceGroup = InstanceGroup()
        instanceGroup.groupName = "master"
        instanceGroup.template = template
        val eventData = CloudbreakEventData(1L, "STACK_CREATED", "Stack created")

        //WHEN
        eventService!!.createStackEvent(eventData)

        //THEN
        BDDMockito.verify(eventRepository).save(captor!!.capture())
        val event = captor.value

        Assert.assertNotNull(event)
        Assert.assertEquals("The user name is not the expected", "John", event.owner)
        Assert.assertEquals("The blueprint name is not the expected", "test-blueprint", event.blueprintName)
        Assert.assertEquals("The blueprint id is not the expected", 1L, event.blueprintId)

    }

}