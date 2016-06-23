package com.sequenceiq.cloudbreak.cloud.aws

import java.util.ArrayList

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.google.common.base.Splitter
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext

@Service
class CloudFormationStackUtil {

    @Value("${cb.max.aws.resource.name.length:}")
    private val maxResourceNameLength: Int = 0

    @Inject
    private val awsClient: AwsClient? = null

    fun getAutoscalingGroupName(ac: AuthenticatedContext, instanceGroup: String, region: String): String {
        val amazonCfClient = awsClient!!.createCloudFormationClient(AwsCredentialView(ac.cloudCredential), region)
        return getAutoscalingGroupName(ac, amazonCfClient, instanceGroup)
    }

    fun getAutoscalingGroupName(ac: AuthenticatedContext, amazonCFClient: AmazonCloudFormationClient, instanceGroup: String): String {
        val cFStackName = getCfStackName(ac)
        val asGroupResource = amazonCFClient.describeStackResource(DescribeStackResourceRequest().withStackName(cFStackName).withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replace("_".toRegex(), ""))))
        return asGroupResource.stackResourceDetail.physicalResourceId
    }

    fun getCfStackName(ac: AuthenticatedContext): String {
        return String.format("%s-%s", String(Splitter.fixedLength(maxResourceNameLength - (ac.cloudContext.id!!.toString().length + 1)).splitToList(ac.cloudContext.name)[0]), ac.cloudContext.id)
    }

    fun getInstanceIds(amazonASClient: AmazonAutoScalingClient, asGroupName: String): List<String> {
        val describeAutoScalingGroupsResult = amazonASClient.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName))
        val instanceIds = ArrayList<String>()
        if (describeAutoScalingGroupsResult.autoScalingGroups[0].instances != null) {
            for (instance in describeAutoScalingGroupsResult.autoScalingGroups[0].instances) {
                instanceIds.add(instance.instanceId)
            }
        }
        return instanceIds
    }

    fun createDescribeInstancesRequest(instanceIds: Collection<String>): DescribeInstancesRequest {
        return DescribeInstancesRequest().withInstanceIds(instanceIds)
    }

}