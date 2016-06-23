package com.sequenceiq.cloudbreak.service.usages

import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specifications
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository
import com.sequenceiq.cloudbreak.repository.FileSystemRepository
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.TemplateRepository

@Service
class DefaultCloudbreakUsageGeneratorService : CloudbreakUsageGeneratorService {

    @Inject
    private val usageRepository: CloudbreakUsageRepository? = null

    @Inject
    private val eventRepository: CloudbreakEventRepository? = null

    @Inject
    private val stackUsageGenerator: StackUsageGenerator? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val templateRepository: TemplateRepository? = null

    @Inject
    private val fileSystemRepository: FileSystemRepository? = null

    @Inject
    private val orchestratorRepository: OrchestratorRepository? = null

    @Scheduled(cron = "0 01 0 * * *")
    override fun generate() {
        val usageList = ArrayList<CloudbreakUsage>()
        val cloudbreakEvents = cloudbreakEvents
        val stackEvents = groupCloudbreakEventsByStack(cloudbreakEvents)
        generateDailyUsageForStacks(usageList, stackEvents)
        val stackIds = HashSet<Long>()
        stackIds.addAll(stackEvents.keys)
        stackIds.addAll(stackRepository!!.findStacksWithoutEvents())
        deleteTerminatedStacks(stackIds)
        usageRepository!!.save(usageList)
    }

    private val cloudbreakEvents: Iterable<CloudbreakEvent>
        get() {
            val cloudbreakEvents: Iterable<CloudbreakEvent>
            val usagesCount = usageRepository!!.count()
            val sortByTimestamp = Sort("eventTimestamp")
            if (usagesCount > 0) {
                val startOfPreviousDay = startOfPreviousDay
                LOGGER.info("Generate usages from events since '{}'.", Date(startOfPreviousDay))
                val sinceSpecification = CloudbreakEventSpecifications.eventsSince(startOfPreviousDay)
                cloudbreakEvents = eventRepository!!.findAll(Specifications.where(sinceSpecification), sortByTimestamp)
            } else {
                LOGGER.info("Generate usages from all events....")
                cloudbreakEvents = eventRepository!!.findAll(sortByTimestamp)
            }
            return cloudbreakEvents
        }

    private val startOfPreviousDay: Long
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private fun groupCloudbreakEventsByStack(userStackEvents: Iterable<CloudbreakEvent>): Map<Long, List<CloudbreakEvent>> {
        val stackIdToCbEventMap = HashMap<Long, List<CloudbreakEvent>>()
        for (cbEvent in userStackEvents) {
            LOGGER.debug("Processing stack {} for user {}", cbEvent.stackId, cbEvent.owner)
            if (!stackIdToCbEventMap.containsKey(cbEvent.stackId)) {
                stackIdToCbEventMap.put(cbEvent.stackId, ArrayList<CloudbreakEvent>())
            }
            stackIdToCbEventMap[cbEvent.stackId].add(cbEvent)
        }
        return stackIdToCbEventMap
    }

    private fun generateDailyUsageForStacks(usageList: MutableList<CloudbreakUsage>, stackEvents: Map<Long, List<CloudbreakEvent>>) {
        for (stackEventEntry in stackEvents.entries) {
            LOGGER.debug("Processing stackId {} for userid {}", stackEventEntry.key)
            val stackDailyUsages = stackUsageGenerator!!.generate(stackEventEntry.value)
            usageList.addAll(stackDailyUsages)
        }
    }

    private fun deleteTerminatedStacks(stackIds: Set<Long>) {
        for (stackId in stackIds) {
            val stack = stackRepository!!.findById(stackId)
            if (stack != null && stack.isDeleteCompleted) {
                var fsId: Long? = null
                if (stack.cluster != null && stack.cluster.fileSystem != null) {
                    fsId = stack.cluster.fileSystem.id
                }
                var orchestratorId: Long? = null
                if (stack.orchestrator != null) {
                    orchestratorId = stack.orchestrator.id
                }
                stackRepository.delete(stack)
                deleteTemplatesOfStack(stack)
                if (fsId != null) {
                    fileSystemRepository!!.delete(fsId)
                }
                if (orchestratorId != null) {
                    orchestratorRepository!!.delete(orchestratorId)
                }
                eventRepository!!.delete(eventRepository.findCloudbreakEventsForStack(stackId))
            }
        }
    }

    private fun deleteTemplatesOfStack(stack: Stack) {
        for (instanceGroup in stack.instanceGroups) {
            val template = instanceGroup.template
            if (template != null) {
                val allStackForTemplate = stackRepository!!.findAllStackForTemplate(template.id)
                if (template.isDeleted && allStackForTemplate.size <= 1) {
                    templateRepository!!.delete(template)
                }
            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsageGeneratorService::class.java)
    }
}
