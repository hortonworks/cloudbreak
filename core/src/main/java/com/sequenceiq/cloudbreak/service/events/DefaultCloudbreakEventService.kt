package com.sequenceiq.cloudbreak.service.events

import java.util.Calendar
import java.util.Collections

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specifications
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications
import com.sequenceiq.cloudbreak.repository.StackRepository

import reactor.bus.Event
import reactor.bus.EventBus
import reactor.bus.selector.Selectors

@Service
class DefaultCloudbreakEventService : CloudbreakEventService {

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val eventRepository: CloudbreakEventRepository? = null

    @Inject
    private val reactor: EventBus? = null

    @Inject
    private val cloudbreakEventHandler: CloudbreakEventHandler? = null

    @PostConstruct
    @Throws(Exception::class)
    fun setup() {
        reactor!!.on(Selectors.`$`<String>(CLOUDBREAK_EVENT), cloudbreakEventHandler)
    }

    override fun fireCloudbreakEvent(stackId: Long?, eventType: String, eventMessage: String) {
        val eventData = CloudbreakEventData(stackId, eventType, eventMessage)
        LOGGER.info("Firing Cloudbreak event: {}", eventData)
        val reactorEvent = Event.wrap(eventData)
        reactor!!.notify(CLOUDBREAK_EVENT, reactorEvent)
    }

    override fun fireCloudbreakInstanceGroupEvent(stackId: Long?, eventType: String, eventMessage: String, instanceGroupName: String) {
        val eventData = InstanceGroupEventData(stackId, eventType, eventMessage, instanceGroupName)
        LOGGER.info("Fireing cloudbreak event: {}", eventData)
        val reactorEvent = Event.wrap(eventData)
        reactor!!.notify(CLOUDBREAK_EVENT, reactorEvent)
    }

    override fun createStackEvent(eventData: CloudbreakEventData): CloudbreakEvent {
        LOGGER.debug("Creating stack event from: {}", eventData)
        val instanceGroupName = getInstanceGroupNameFromEvent(eventData)
        val stack = stackRepository!!.findById(eventData.entityId)
        var stackEvent = createStackEvent(stack, eventData.eventType, eventData.eventMessage, instanceGroupName)
        stackEvent = eventRepository!!.save(stackEvent)
        LOGGER.info("Created stack event: {}", stackEvent)
        return stackEvent
    }

    private fun getInstanceGroupNameFromEvent(eventData: CloudbreakEventData): String {
        var instanceGroup: String? = null
        if (eventData is InstanceGroupEventData) {
            instanceGroup = eventData.instanceGroupName
        }
        return instanceGroup
    }

    @SuppressWarnings("unchecked")
    override fun cloudbreakEvents(owner: String, since: Long?): List<CloudbreakEvent> {
        var events: List<CloudbreakEvent>? = null
        if (null == since) {
            events = eventRepository!!.findAll(CloudbreakEventSpecifications.eventsForUser(owner))
        } else {
            events = eventRepository!!.findAll(Specifications.where(CloudbreakEventSpecifications.eventsForUser(owner)).and(CloudbreakEventSpecifications.eventsSince(since)))
        }
        return if (null != events) events else Collections.EMPTY_LIST
    }

    private fun createStackEvent(stack: Stack, eventType: String, eventMessage: String, instanceGroupName: String?): CloudbreakEvent {
        val stackEvent = CloudbreakEvent()

        stackEvent.eventTimestamp = Calendar.getInstance().time
        stackEvent.eventMessage = eventMessage
        stackEvent.eventType = eventType
        stackEvent.owner = stack.owner
        stackEvent.account = stack.account
        stackEvent.stackId = stack.id
        stackEvent.stackStatus = stack.status
        stackEvent.stackName = stack.name
        stackEvent.nodeCount = stack.runningInstanceMetaData.size
        stackEvent.region = stack.region
        stackEvent.availabilityZone = stack.availabilityZone
        stackEvent.cloud = stack.cloudPlatform()

        populateClusterData(stackEvent, stack)

        if (instanceGroupName != null) {
            stackEvent.instanceGroup = instanceGroupName
        }

        return stackEvent
    }

    private fun populateClusterData(stackEvent: CloudbreakEvent, stack: Stack) {
        val cluster = stack.cluster
        if (cluster != null) {
            stackEvent.clusterStatus = cluster.status
            if (cluster.blueprint != null) {
                stackEvent.blueprintId = cluster.blueprint.id!!
                stackEvent.blueprintName = cluster.blueprint.blueprintName
            }
            stackEvent.clusterId = cluster.id
            stackEvent.clusterName = cluster.name
        } else {
            LOGGER.debug("No cluster data available for the stack: {}", stack.id)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DefaultCloudbreakEventService::class.java)
        private val CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT"
    }
}
