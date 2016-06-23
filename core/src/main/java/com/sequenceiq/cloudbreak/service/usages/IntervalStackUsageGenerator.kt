package com.sequenceiq.cloudbreak.service.usages

import com.sequenceiq.cloudbreak.cloud.model.Platform.platform

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.price.PriceGenerator

@Component
class IntervalStackUsageGenerator {

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val instanceUsageGenerator: IntervalInstanceUsageGenerator? = null

    @Inject
    private val priceGenerators: List<PriceGenerator>? = null

    @Throws(ParseException::class)
    fun generateUsages(startTime: Date, stopTime: Date, startEvent: CloudbreakEvent): List<CloudbreakUsage> {
        val dailyUsagesByInstanceGroup = ArrayList<CloudbreakUsage>()
        val stack = stackRepository!!.findById(startEvent.stackId)

        if (stack != null) {
            for (instanceGroup in stack.instanceGroups) {
                val instanceGroupDailyUsages = HashMap<String, CloudbreakUsage>()
                val template = instanceGroup.template
                val instanceType = getInstanceType(template)
                val groupName = instanceGroup.groupName
                val priceGenerator = selectPriceGeneratorByPlatform(Companion.platform(template.cloudPlatform()))

                for (metaData in instanceGroup.allInstanceMetaData) {
                    val instanceHours = instanceUsageGenerator!!.getInstanceHours(metaData, startTime, stopTime)
                    addInstanceHoursToStackUsages(instanceGroupDailyUsages, instanceHours, startEvent, instanceType, groupName)
                }

                addCalculatedPrice(instanceGroupDailyUsages, priceGenerator, template)
                dailyUsagesByInstanceGroup.addAll(instanceGroupDailyUsages.values)
            }
        }
        return dailyUsagesByInstanceGroup
    }

    private fun getInstanceType(template: Template): String {
        return template.instanceType
    }

    private fun selectPriceGeneratorByPlatform(cloudPlatform: Platform): PriceGenerator {
        var result: PriceGenerator? = null
        for (generator in priceGenerators!!) {
            val generatorCloudPlatform = generator.cloudPlatform
            if (cloudPlatform == generatorCloudPlatform) {
                result = generator
                break
            }
        }
        return result
    }

    @Throws(ParseException::class)
    private fun addInstanceHoursToStackUsages(dailyStackUsages: MutableMap<String, CloudbreakUsage>, instanceUsages: Map<String, Long>,
                                              event: CloudbreakEvent, instanceType: String, groupName: String) {

        for ((day, instanceHours) in instanceUsages) {
            if (dailyStackUsages.containsKey(day)) {
                val usage = dailyStackUsages[day]
                val numberOfHours = usage.instanceHours!! + instanceHours
                usage.instanceHours = numberOfHours
            } else {
                val usage = getCloudbreakUsage(event, instanceHours, day, instanceType, groupName)
                dailyStackUsages.put(day, usage)
            }
        }
    }

    @Throws(ParseException::class)
    private fun getCloudbreakUsage(event: CloudbreakEvent, instanceHours: Long, dayString: String, instanceType: String, groupName: String): CloudbreakUsage {
        val day = DATE_FORMAT.parse(dayString)
        val usage = CloudbreakUsage()
        usage.owner = event.owner
        usage.account = event.account
        usage.provider = event.cloud
        usage.region = event.region
        usage.availabilityZone = event.availabilityZone
        usage.instanceHours = instanceHours
        usage.day = day
        usage.stackId = event.stackId
        usage.stackName = event.stackName
        usage.instanceType = instanceType
        usage.instanceGroup = groupName
        return usage
    }

    private fun addCalculatedPrice(instanceGroupDailyUsages: Map<String, CloudbreakUsage>, priceGenerator: PriceGenerator, template: Template) {
        for (usage in instanceGroupDailyUsages.values) {
            val instanceHours = usage.instanceHours
            val costs = calculateCostOfInstance(priceGenerator, template, instanceHours)
            usage.costs = costs
        }
    }

    private fun calculateCostOfInstance(priceGenerator: PriceGenerator?, template: Template, instanceHours: Long?): Double? {
        var result: Double? = 0.0
        if (priceGenerator != null) {
            result = priceGenerator.calculate(template, instanceHours)
        }
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(IntervalStackUsageGenerator::class.java)
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
    }
}
