package com.sequenceiq.periscope.model

import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.api.records.ApplicationReport
import org.apache.hadoop.yarn.api.records.YarnApplicationState

class SchedulerApplication(appReport: ApplicationReport, var priority: Priority?) {

    val applicationId: ApplicationId
    val startTime: Long
    var isMoved: Boolean = false
    var progress: Double = 0.toDouble()
        private set
    var state: YarnApplicationState? = null

    init {
        this.applicationId = appReport.applicationId
        this.startTime = appReport.startTime
    }

    fun update(report: ApplicationReport) {
        this.progress = report.progress.toDouble()
        this.state = report.yarnApplicationState
    }

}
