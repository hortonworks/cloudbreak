package com.sequenceiq.periscope.rest.converter

import org.apache.hadoop.yarn.api.records.ApplicationReport
import org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReport
import org.springframework.stereotype.Component

import com.sequenceiq.periscope.rest.json.AppReportJson

@Component
class AppReportConverter : AbstractConverter<AppReportJson, ApplicationReport>() {

    override fun convert(source: ApplicationReport): AppReportJson {
        val json = AppReportJson()
        json.appId = source.applicationId.toString()
        json.start = source.startTime
        json.finish = source.finishTime
        json.progress = source.progress
        json.queue = source.queue
        json.url = source.trackingUrl
        json.user = source.user
        json.state = source.yarnApplicationState.name
        val usageReport = source.applicationResourceUsageReport
        json.reservedContainers = usageReport.numReservedContainers
        json.usedContainers = usageReport.numUsedContainers
        json.usedMemory = usageReport.usedResources.memory
        json.usedVCores = usageReport.usedResources.virtualCores
        return json
    }

}
