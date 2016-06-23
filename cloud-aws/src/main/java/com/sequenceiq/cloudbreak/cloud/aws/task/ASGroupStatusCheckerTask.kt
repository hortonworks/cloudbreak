package com.sequenceiq.cloudbreak.cloud.aws.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.Activity
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult
import com.amazonaws.services.ec2.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

@Component(ASGroupStatusCheckerTask.NAME)
@Scope(value = "prototype")
class ASGroupStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val autoScalingGroupName: String, private val requiredInstances: Int?, private val awsClient: AwsClient,
                               private val cloudFormationStackUtil: CloudFormationStackUtil) : PollBooleanStateTask(authenticatedContext, true) {

    override fun call(): Boolean? {
        LOGGER.info("Checking status of Auto Scaling group '{}'", autoScalingGroupName)
        val amazonEC2Client = awsClient.createAccess(AwsCredentialView(authenticatedContext.cloudCredential),
                authenticatedContext.cloudContext.location!!.region.value())
        val amazonASClient = awsClient.createAutoScalingClient(AwsCredentialView(authenticatedContext.cloudCredential),
                authenticatedContext.cloudContext.location!!.region.value())
        val instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, autoScalingGroupName)
        if (instanceIds.size < requiredInstances) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size, requiredInstances)
            val describeScalingActivitiesRequest = DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName)
            val activities = amazonASClient.describeScalingActivities(describeScalingActivitiesRequest).activities
            for (activity in activities) {
                if (activity.progress == COMPLETED && CANCELLED == activity.statusCode) {
                    throw CancellationException(activity.statusMessage)
                }
            }
            return false
        }
        val describeResult = amazonEC2Client.describeInstanceStatus(DescribeInstanceStatusRequest().withInstanceIds(instanceIds))
        if (describeResult.instanceStatuses.size < requiredInstances) {
            LOGGER.debug("Instances up: {}, needed: {}", describeResult.instanceStatuses.size, requiredInstances)
            return false
        }
        for (status in describeResult.instanceStatuses) {
            if (INSTANCE_RUNNING != status.instanceState.code) {
                LOGGER.debug("Instances are up but not all of them are in running state.")
                return false
            }
        }
        return true
    }

    companion object {
        val NAME = "aSGroupStatusCheckerTask"

        private val INSTANCE_RUNNING = 16
        private val COMPLETED = 100
        private val CANCELLED = "Cancelled"
        private val LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask::class.java)
    }

}
