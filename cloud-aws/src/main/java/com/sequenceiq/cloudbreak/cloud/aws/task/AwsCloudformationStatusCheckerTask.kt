package com.sequenceiq.cloudbreak.cloud.aws.task

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.model.StackEvent
import com.amazonaws.services.cloudformation.model.StackStatus
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

@Component(AwsCloudformationStatusCheckerTask.NAME)
@Scope(value = "prototype")
class AwsCloudformationStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val client: AmazonCloudFormationClient,
                                         private val successStatus: StackStatus, private val errorStatus: StackStatus, private val stackErrorStatuses: List<StackStatus>, private val cloudFormationStackName: String, cancellable: Boolean) : PollBooleanStateTask(authenticatedContext, cancellable) {

    override fun call(): Boolean? {
        var result = false
        val describeStacksRequest = DescribeStacksRequest().withStackName(cloudFormationStackName)
        val stackEventsRequest = DescribeStackEventsRequest().withStackName(cloudFormationStackName)
        LOGGER.info("Checking if AWS CloudFormation stack '{}' reached status '{}'", cloudFormationStackName, successStatus)

        try {
            val cfStack = client.describeStacks(describeStacksRequest).stacks[0]
            val stackEvents = client.describeStackEvents(stackEventsRequest).stackEvents
            if (!stackEvents.isEmpty() && cfStack != null) {
                val cfStackStatus = StackStatus.valueOf(cfStack.stackStatus)
                if (stackErrorStatuses.contains(cfStackStatus)) {
                    val errorStatusReason = getErrorCauseStatusReason(stackEvents, errorStatus)
                    throw CloudConnectorException(String.format("AWS CloudFormation stack reached an error state: %s reason: %s", errorStatus.toString(),
                            errorStatusReason))
                } else if (cfStackStatus == successStatus) {
                    result = true
                }
            }
        } catch (e: AmazonServiceException) {
            LOGGER.warn("Could not get the description of CloudFormation stack: {}", e.errorMessage)
            result = true
        }

        return result
    }

    private fun getErrorCauseStatusReason(stackEvents: List<StackEvent>, errorStatus: StackStatus): String {
        var cause: StackEvent? = null
        for (event in stackEvents) {
            if (event.resourceStatus == errorStatus.toString()) {
                if (cause == null) {
                    cause = event
                } else if (cause.timestamp.time > event.timestamp.time) {
                    cause = event
                }
            }
        }
        return cause!!.resourceStatusReason
    }

    companion object {
        val NAME = "awsCloudformationStatusCheckerTask"

        private val LOGGER = LoggerFactory.getLogger(AwsCloudformationStatusCheckerTask::class.java)
    }
}
