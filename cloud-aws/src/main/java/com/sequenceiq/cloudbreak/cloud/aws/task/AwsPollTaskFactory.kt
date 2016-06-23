package com.sequenceiq.cloudbreak.cloud.aws.task

import javax.inject.Inject

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.StackStatus
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateSnapshotResult
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.task.PollTask

@Component
class AwsPollTaskFactory {
    @Inject
    private val applicationContext: ApplicationContext? = null

    fun newAwsCloudformationStatusCheckerTask(authenticatedContext: AuthenticatedContext, client: AmazonCloudFormationClient,
                                              successStatus: StackStatus, errorStatus: StackStatus, stackErrorStatuses: List<StackStatus>, cloudFormationStackName: String, cancellable: Boolean): PollTask<Boolean> {
        return createPollTask(AwsCloudformationStatusCheckerTask.NAME, authenticatedContext, client, successStatus, errorStatus,
                stackErrorStatuses, cloudFormationStackName, cancellable)
    }

    fun newEbsVolumeStatusCheckerTask(authenticatedContext: AuthenticatedContext, stack: CloudStack,
                                      amazonEC2Client: AmazonEC2Client, volumeId: String): PollTask<Boolean> {
        return createPollTask(EbsVolumeStatusCheckerTask.NAME, authenticatedContext, stack, amazonEC2Client, volumeId)
    }

    fun newCreateSnapshotReadyStatusCheckerTask(authenticatedContext: AuthenticatedContext, snapshotResult: CreateSnapshotResult,
                                                snapshotId: String, ec2Client: AmazonEC2Client): PollTask<Boolean> {
        return createPollTask(CreateSnapshotReadyStatusCheckerTask.NAME, authenticatedContext, snapshotResult, snapshotId, ec2Client)
    }

    fun newASGroupStatusCheckerTask(authenticatedContext: AuthenticatedContext, asGroupName: String, requiredInstances: Int?,
                                    awsClient: AwsClient, cloudFormationStackUtil: CloudFormationStackUtil): PollTask<Boolean> {
        return createPollTask(ASGroupStatusCheckerTask.NAME, authenticatedContext, asGroupName, requiredInstances, awsClient, cloudFormationStackUtil)
    }

    @SuppressWarnings("unchecked")
    private fun <T> createPollTask(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }
}
