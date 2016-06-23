package com.sequenceiq.cloudbreak.cloud.aws

import java.io.UnsupportedEncodingException
import java.net.URLDecoder

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest
import com.amazonaws.services.ec2.model.DescribeSubnetsResult
import com.amazonaws.services.ec2.model.DescribeVpcAttributeRequest
import com.amazonaws.services.ec2.model.InternetGateway
import com.amazonaws.services.ec2.model.InternetGatewayAttachment
import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.VpcAttributeName
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.model.AttachedPolicy
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest
import com.amazonaws.services.identitymanagement.model.GetPolicyResult
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult
import com.amazonaws.services.identitymanagement.model.GetRoleRequest
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesResult
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesResult
import com.amazonaws.util.json.JSONArray
import com.amazonaws.util.json.JSONException
import com.amazonaws.util.json.JSONObject
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult


@Component
class AwsSetup : Setup {

    @Value("${cb.aws.spotinstances.enabled:}")
    private val awsSpotinstanceEnabled: Boolean = false

    @Inject
    private val cfStackUtil: CloudFormationStackUtil? = null

    @Inject
    private val awsClient: AwsClient? = null

    override fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image): ImageStatusResult {
        return ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE)
    }

    override fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image) {
        LOGGER.debug("prepare image has been executed")
    }

    override fun prerequisites(ac: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {
        val awsNetworkView = AwsNetworkView(stack.network)
        val credentialView = AwsCredentialView(ac.cloudCredential)
        val region = ac.cloudContext.location!!.region.value()
        if (!awsSpotinstanceEnabled) {
            for (group in stack.groups) {
                if (group.instances[0].getParameter<Double>("spotPrice", Double::class.java) != null) {
                    throw CloudConnectorException(String.format("Spot instances are not supported on this AMI: %s", stack.image))
                }
            }
        }
        val awsCredentialView = AwsCredentialView(ac.cloudCredential)
        val awsInstanceProfileView = AwsInstanceProfileView(stack.parameters)
        if (awsClient!!.roleBasedCredential(awsCredentialView) && awsInstanceProfileView.isCreateInstanceProfile) {
            validateInstanceProfileCreation(awsCredentialView)
        }
        if (awsNetworkView.isExistingVPC) {
            try {
                val amazonEC2Client = awsClient.createAccess(credentialView, region)
                validateExistingVpc(awsNetworkView, amazonEC2Client)
                validateExistingIGW(awsNetworkView, amazonEC2Client)
                validateExistingSubnet(awsNetworkView, amazonEC2Client)
            } catch (e: AmazonServiceException) {
                throw CloudConnectorException(e.errorMessage)
            } catch (e: AmazonClientException) {
                throw CloudConnectorException(e.message)
            }

        }
        validateExistingKeyPair(ac, credentialView, region)
        LOGGER.debug("setup has been executed")
    }

    private fun validateInstanceProfileCreation(awsCredentialView: AwsCredentialView) {
        val roleRequest = GetRoleRequest()
        val roleName = awsCredentialView.roleArn.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1]
        LOGGER.info("Start validate {} role for S3 access.", roleName)
        roleRequest.withRoleName(roleName)
        val client = awsClient!!.createAmazonIdentityManagement(awsCredentialView)
        try {
            val listRolePoliciesRequest = ListRolePoliciesRequest()
            listRolePoliciesRequest.roleName = roleName
            val listRolePoliciesResult = client.listRolePolicies(listRolePoliciesRequest)
            for (s in listRolePoliciesResult.policyNames) {
                if (checkIamOrS3Statement(roleName, client, s)) {
                    LOGGER.info("Validation successful for s3 or iam access.")
                    return
                }
            }
            val listAttachedRolePoliciesRequest = ListAttachedRolePoliciesRequest()
            listAttachedRolePoliciesRequest.roleName = roleName
            val listAttachedRolePoliciesResult = client.listAttachedRolePolicies(listAttachedRolePoliciesRequest)
            for (attachedPolicy in listAttachedRolePoliciesResult.attachedPolicies) {
                if (checkIamOrS3Access(client, attachedPolicy)) {
                    LOGGER.info("Validation successful for s3 or iam access.")
                    return
                }
            }
        } catch (ase: AmazonServiceException) {
            if (ase.statusCode == UNAUTHORIZED) {
                val policyMEssage = "Could not get policies on the role because the arn role do not have enough permission: %s"
                LOGGER.info(String.format(policyMEssage, ase.errorMessage))
                throw CloudConnectorException(String.format(policyMEssage, ase.errorMessage))
            } else {
                LOGGER.info(ase.message)
                throw CloudConnectorException(ase.errorMessage)
            }
        } catch (e: Exception) {
            LOGGER.info(e.message)
            throw CloudConnectorException(e.message)
        }

        LOGGER.info("Could not get policies on the role because the arn role do not have enough permission.")
        throw CloudConnectorException("Could not get policies on the role because the arn role do not have enough permission.")
    }

    @Throws(UnsupportedEncodingException::class, JSONException::class)
    private fun checkIamOrS3Statement(roleName: String, client: AmazonIdentityManagement, s: String): Boolean {
        val getRolePolicyRequest = GetRolePolicyRequest()
        getRolePolicyRequest.roleName = roleName
        getRolePolicyRequest.policyName = s
        val rolePolicy = client.getRolePolicy(getRolePolicyRequest)
        val decode = URLDecoder.decode(rolePolicy.policyDocument, "UTF-8")
        val `object` = JSONObject(decode)
        val statement = `object`.getJSONArray("Statement")
        for (i in 0..statement.length() - 1) {
            val action = statement.getJSONObject(i).getJSONArray("Action")
            for (j in 0..action.length() - 1) {
                val actionEntry = action.get(j).toString().replace(" ".toRegex(), "").toLowerCase()
                if ("iam:createrole" == actionEntry || "iam:*" == actionEntry) {
                    LOGGER.info("Role has able to operate on iam resources: {}.", action.get(j).toString())
                    return true
                }
            }
        }
        return false
    }

    private fun checkIamOrS3Access(client: AmazonIdentityManagement, attachedPolicy: AttachedPolicy): Boolean {
        val getRolePolicyRequest = GetPolicyRequest()
        getRolePolicyRequest.policyArn = attachedPolicy.policyArn
        val policy = client.getPolicy(getRolePolicyRequest)
        if (policy.policy.arn.toLowerCase().contains("iam")) {
            LOGGER.info("Role has policy for iam resources: {}.", policy.policy.arn)
            return true
        }
        return false
    }

    private fun validateExistingVpc(awsNetworkView: AwsNetworkView, amazonEC2Client: AmazonEC2Client) {
        val describeVpcAttributeRequest = DescribeVpcAttributeRequest()
        describeVpcAttributeRequest.withVpcId(awsNetworkView.existingVPC)
        describeVpcAttributeRequest.withAttribute(VpcAttributeName.EnableDnsSupport)
        amazonEC2Client.describeVpcAttribute(describeVpcAttributeRequest)
    }

    private fun validateExistingSubnet(awsNetworkView: AwsNetworkView, amazonEC2Client: AmazonEC2Client) {
        if (awsNetworkView.isExistingSubnet) {
            val describeSubnetsRequest = DescribeSubnetsRequest()
            describeSubnetsRequest.withSubnetIds(awsNetworkView.existingSubnet)
            val describeSubnetsResult = amazonEC2Client.describeSubnets(describeSubnetsRequest)
            if (describeSubnetsResult.subnets.size < 1) {
                throw CloudConnectorException(String.format(SUBNET_DOES_NOT_EXIST_MSG, awsNetworkView.existingSubnet))
            } else {
                val subnet = describeSubnetsResult.subnets[0]
                val vpcId = subnet.vpcId
                if (vpcId != null && vpcId != awsNetworkView.existingVPC) {
                    throw CloudConnectorException(String.format(SUBNETVPC_DOES_NOT_EXIST_MSG, awsNetworkView.existingSubnet,
                            awsNetworkView.existingVPC))
                }
            }
        }
    }

    private fun validateExistingIGW(awsNetworkView: AwsNetworkView, amazonEC2Client: AmazonEC2Client) {
        if (awsNetworkView.isExistingIGW) {
            val describeInternetGatewaysRequest = DescribeInternetGatewaysRequest()
            describeInternetGatewaysRequest.withInternetGatewayIds(awsNetworkView.existingIGW)
            val describeInternetGatewaysResult = amazonEC2Client.describeInternetGateways(describeInternetGatewaysRequest)
            if (describeInternetGatewaysResult.internetGateways.size < 1) {
                throw CloudConnectorException(String.format(IGW_DOES_NOT_EXIST_MSG, awsNetworkView.existingIGW))
            } else {
                val internetGateway = describeInternetGatewaysResult.internetGateways[0]
                val attachment = internetGateway.attachments[0]
                if (attachment != null && attachment.vpcId != awsNetworkView.existingVPC) {
                    throw CloudConnectorException(String.format(IGWVPC_DOES_NOT_EXIST_MSG, awsNetworkView.existingIGW,
                            awsNetworkView.existingVPC))
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {
    }

    private fun validateExistingKeyPair(authenticatedContext: AuthenticatedContext, credentialView: AwsCredentialView, region: String) {
        val keyPairName = awsClient!!.getExistingKeyPairName(authenticatedContext)
        if (StringUtils.isNoneEmpty(keyPairName)) {
            var keyPairIsPresentOnEC2 = false
            try {
                val client = awsClient.createAccess(credentialView, region)
                val describeKeyPairsResult = client.describeKeyPairs(DescribeKeyPairsRequest().withKeyNames(keyPairName))
                keyPairIsPresentOnEC2 = describeKeyPairsResult.keyPairs.stream().findFirst().isPresent()
            } catch (e: Exception) {
                val errorMessage = String.format("Failed to get the key pair [name: '%s'] from EC2 [roleArn:'%s'], detailed message: %s.",
                        keyPairName, credentialView.roleArn, e.message)
                LOGGER.error(errorMessage, e)
            }

            if (!keyPairIsPresentOnEC2) {
                throw CloudConnectorException(String.format("The key pair '%s' could not be found in the '%s' region of EC2.", keyPairName, region))
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AwsSetup::class.java)
        private val IGW_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not exist or belongs to a different region."
        private val SUBNET_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not exist or belongs to a different region."
        private val SUBNETVPC_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not belong to the given VPC '%s'."
        private val IGWVPC_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not belong to the given VPC '%s'."
        private val FINISHED_PROGRESS_VALUE = 100
        private val UNAUTHORIZED = 403
    }
}
