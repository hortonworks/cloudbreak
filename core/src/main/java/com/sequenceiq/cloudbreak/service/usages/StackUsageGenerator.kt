package com.sequenceiq.cloudbreak.service.usages

import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.SECOND

import java.text.ParseException
import java.util.Calendar
import java.util.Date
import java.util.LinkedList

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository

@Component
class StackUsageGenerator {

    @Inject
    private val eventRepository: CloudbreakEventRepository? = null

    @Inject
    private val intervalUsageGenerator: IntervalStackUsageGenerator? = null

    fun generate(stackEvents: List<CloudbreakEvent>): List<CloudbreakUsage> {
        val stackUsages = LinkedList<CloudbreakUsage>()
        var actEvent: CloudbreakEvent? = null
        try {
            var start: CloudbreakEvent? = null
            for (cbEvent in stackEvents) {
                actEvent = cbEvent
                if (isStartEvent(cbEvent) && start == null) {
                    start = cbEvent
                } else if (validStopEvent(start, cbEvent)) {
                    addAllGeneratedUsages(stackUsages, start, cbEvent.eventTimestamp)
                    start = null
                }
            }

            generateRunningStackUsage(stackUsages, start)
        } catch (e: Exception) {
            LOGGER.error("Usage generation failed for stack(id:{})! Error when processing event(id:{})! Ex: {}", actEvent!!.stackId, actEvent.id, e)
            throw IllegalStateException(e)
        }

        return stackUsages
    }

    private fun validStopEvent(start: CloudbreakEvent?, cbEvent: CloudbreakEvent): Boolean {
        return start != null && start.eventTimestamp.before(cbEvent.eventTimestamp) && isStopEvent(cbEvent)
    }

    private fun isStopEvent(event: CloudbreakEvent): Boolean {
        return BillingStatus.BILLING_STOPPED.name == event.eventType
    }

    private fun isStartEvent(event: CloudbreakEvent): Boolean {
        return BillingStatus.BILLING_STARTED.name == event.eventType
    }

    @Throws(ParseException::class)
    private fun addAllGeneratedUsages(stackUsages: MutableList<CloudbreakUsage>, startEvent: CloudbreakEvent, stopTime: Date) {
        val usages = intervalUsageGenerator!!.generateUsages(startEvent.eventTimestamp, stopTime, startEvent)
        stackUsages.addAll(usages)
    }

    @Throws(ParseException::class)
    private fun generateRunningStackUsage(dailyCbUsages: MutableList<CloudbreakUsage>, startEvent: CloudbreakEvent?) {
        if (startEvent != null) {
            val cal = Calendar.getInstance()
            setDayToBeginning(cal)
            addAllGeneratedUsages(dailyCbUsages, startEvent, cal.time)

            //get overflowed minutes from the start event
            val start = Calendar.getInstance()
            start.time = startEvent.eventTimestamp
            cal.set(MINUTE, start.get(MINUTE))
            //saveOne billing start event for daily usage generation
            val newBillingStart = createBillingStarterCloudbreakEvent(startEvent, cal)
            eventRepository!!.save(newBillingStart)
            LOGGER.debug("BILLING_STARTED is created with date:{} for running stack {}.", cal.time, newBillingStart.stackId)
        }
    }

    private fun setDayToBeginning(calendar: Calendar) {
        calendar.set(HOUR_OF_DAY, 0)
        calendar.set(MINUTE, 0)
        calendar.set(SECOND, 0)
        calendar.set(MILLISECOND, 0)
    }

    private fun createBillingStarterCloudbreakEvent(startEvent: CloudbreakEvent, cal: Calendar): CloudbreakEvent {
        val event = CloudbreakEvent()
        event.eventType = BillingStatus.BILLING_STARTED.name
        event.account = startEvent.account
        event.owner = startEvent.owner
        event.eventMessage = startEvent.eventMessage
        event.blueprintId = startEvent.blueprintId
        event.blueprintName = startEvent.blueprintName
        event.eventTimestamp = cal.time
        event.cloud = startEvent.cloud
        event.region = startEvent.region
        event.availabilityZone = startEvent.availabilityZone
        event.stackId = startEvent.stackId
        event.stackStatus = startEvent.stackStatus
        event.stackName = startEvent.stackName
        event.nodeCount = startEvent.nodeCount
        event.instanceGroup = startEvent.instanceGroup
        return event
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackUsageGenerator::class.java)
    }
}
