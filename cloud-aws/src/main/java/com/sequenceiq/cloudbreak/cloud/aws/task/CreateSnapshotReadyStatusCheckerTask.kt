package com.sequenceiq.cloudbreak.cloud.aws.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateSnapshotResult
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

@Component(CreateSnapshotReadyStatusCheckerTask.NAME)
@Scope(value = "prototype")
class CreateSnapshotReadyStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val snapshotResult: CreateSnapshotResult, private val snapshotId: String,
                                           private val ec2Client: AmazonEC2Client) : PollBooleanStateTask(authenticatedContext, true) {

    private val authenticatedContext: AuthenticatedContext? = null

    override fun call(): Boolean? {
        LOGGER.info("Checking if AWS EBS snapshot '{}' is ready.", snapshotId)
        val result = ec2Client.describeSnapshots(DescribeSnapshotsRequest().withSnapshotIds(snapshotId))
        return result.snapshots != null && !result.snapshots.isEmpty() && "completed" == result.snapshots[0].state
    }

    companion object {
        val NAME = "createSnapshotReadyStatusCheckerTask"

        private val LOGGER = LoggerFactory.getLogger(CreateSnapshotReadyStatusCheckerTask::class.java)
    }
}
