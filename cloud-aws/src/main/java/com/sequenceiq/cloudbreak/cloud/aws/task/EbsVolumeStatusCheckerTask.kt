package com.sequenceiq.cloudbreak.cloud.aws.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeVolumesRequest
import com.amazonaws.services.ec2.model.DescribeVolumesResult
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

@Component(EbsVolumeStatusCheckerTask.NAME)
@Scope(value = "prototype")
class EbsVolumeStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val stack: CloudStack, private val amazonEC2Client: AmazonEC2Client, private val volumeId: String) : PollBooleanStateTask(authenticatedContext, true) {

    private val authenticatedContext: AuthenticatedContext? = null

    override fun call(): Boolean? {
        LOGGER.info("Checking if AWS EBS volume '{}' is created.", volumeId)
        val describeVolumesResult = amazonEC2Client.describeVolumes(DescribeVolumesRequest().withVolumeIds(volumeId))
        return describeVolumesResult.volumes != null && !describeVolumesResult.volumes.isEmpty()
                && "available" == describeVolumesResult.volumes[0].state
    }

    companion object {
        val NAME = "ebsVolumeStatusCheckerTask"

        private val LOGGER = LoggerFactory.getLogger(EbsVolumeStatusCheckerTask::class.java)
    }
}
