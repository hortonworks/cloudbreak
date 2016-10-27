package com.sequenceiq.cloudbreak.cloud.aws;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeVpcAttributeResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.VpcAttributeName;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.GetPolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetPolicyResult;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesResult;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;


@Component
public class AwsSetup implements Setup {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSetup.class);
    private static final String IGW_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not exist or belongs to a different region.";
    private static final String SUBNET_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not exist or belongs to a different region.";
    private static final String SUBNETVPC_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not belong to the given VPC '%s'.";
    private static final String IGWVPC_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not belong to the given VPC '%s'.";
    private static final String IMAGE_OPT_IN_REQUIRED_MSG = "Unable to create cluster because AWS Marketplace subscription to the Hortonworks Data Cloud"
            + " HDP Services is required. In order to create a cluster, you need to accept terms and subscribe to the AWS Marketplace product.";
    private static final String LINK_TO_MARKETPLACE_MSG = "To do so please visit ";
    private static final String MARKETPLACE_HTTP_LINK = "http://aws.amazon.com/marketplace";
    private static final int FINISHED_PROGRESS_VALUE = 100;
    private static final int UNAUTHORIZED = 403;

    @Value("${cb.aws.spotinstances.enabled:}")
    private boolean awsSpotinstanceEnabled;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, Image image) {
        return new ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE);
    }

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, Image image) {
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        verifySpotInstances(stack);
        AwsCredentialView awsCredentialView = new AwsCredentialView(ac.getCloudCredential());
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        validateImageOptIn(awsCredentialView, region, stack.getImage().getImageName());
        if (awsClient.roleBasedCredential(awsCredentialView) && awsInstanceProfileView.isCreateInstanceProfile()) {
            validateInstanceProfileCreation(awsCredentialView);
        }
        if (awsNetworkView.isExistingVPC()) {
            try {
                AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, region);
                validateExistingVpc(awsNetworkView, amazonEC2Client);
                validateExistingIGW(awsNetworkView, amazonEC2Client);
                validateExistingSubnet(awsNetworkView, amazonEC2Client);
            } catch (AmazonServiceException e) {
                throw new CloudConnectorException(e.getErrorMessage());
            } catch (AmazonClientException e) {
                throw new CloudConnectorException(e.getMessage());
            }

        }
        validateExistingKeyPair(ac, credentialView, region);
        LOGGER.debug("setup has been executed");
    }

    private void verifySpotInstances(CloudStack stack) {
        if (!awsSpotinstanceEnabled) {
            for (Group group : stack.getGroups()) {
                if (group.getInstances() != null
                        && !group.getInstances().isEmpty()
                        && new AwsInstanceView(group.getInstances().get(0).getTemplate()).getSpotPrice() != null) {
                    throw new CloudConnectorException(String.format("Spot instances are not supported on this AMI: %s", stack.getImage()));
                }
            }
        }
    }

    private void validateImageOptIn(AwsCredentialView credentialView, String region, String imageName) {
        try {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, region);
            RunInstancesRequest request = new RunInstancesRequest()
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withImageId(imageName)
                    .withInstanceType(InstanceType.M3Large);
            amazonEC2Client.dryRun(request);
            LOGGER.info("Dry run succeeded, AMI '{}' is safe to launch.", imageName);
        } catch (AmazonServiceException e) {
            String errorMessage = e.getErrorMessage();
            if (e.getErrorCode().equals("OptInRequired")) {
                int marketplaceLinkIndex = errorMessage.indexOf(MARKETPLACE_HTTP_LINK);
                if (marketplaceLinkIndex != -1) {
                    errorMessage = IMAGE_OPT_IN_REQUIRED_MSG + " " + LINK_TO_MARKETPLACE_MSG + errorMessage.substring(marketplaceLinkIndex);
                } else {
                    errorMessage = IMAGE_OPT_IN_REQUIRED_MSG;
                }
                throw new CloudConnectorException(errorMessage, e);
            } else {
                LOGGER.error(String.format("Image opt-in could not be validated for AMI '%s'.", imageName), e);
            }
        }
    }

    private void validateInstanceProfileCreation(AwsCredentialView awsCredentialView) {
        GetRoleRequest roleRequest = new GetRoleRequest();
        String roleName = awsCredentialView.getRoleArn().split("/")[1];
        LOGGER.info("Start validate {} role for S3 access.", roleName);
        roleRequest.withRoleName(roleName);
        AmazonIdentityManagement client = awsClient.createAmazonIdentityManagement(awsCredentialView);
        try {
            ListRolePoliciesRequest listRolePoliciesRequest = new ListRolePoliciesRequest();
            listRolePoliciesRequest.setRoleName(roleName);
            ListRolePoliciesResult listRolePoliciesResult = client.listRolePolicies(listRolePoliciesRequest);
            for (String s : listRolePoliciesResult.getPolicyNames()) {
                if (checkIamOrS3Statement(roleName, client, s)) {
                    LOGGER.info("Validation successful for s3 or iam access.");
                    return;
                }
            }
            ListAttachedRolePoliciesRequest listAttachedRolePoliciesRequest = new ListAttachedRolePoliciesRequest();
            listAttachedRolePoliciesRequest.setRoleName(roleName);
            ListAttachedRolePoliciesResult listAttachedRolePoliciesResult = client.listAttachedRolePolicies(listAttachedRolePoliciesRequest);
            for (AttachedPolicy attachedPolicy : listAttachedRolePoliciesResult.getAttachedPolicies()) {
                if (checkIamOrS3Access(client, attachedPolicy)) {
                    LOGGER.info("Validation successful for s3 or iam access.");
                    return;
                }
            }
        } catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == UNAUTHORIZED) {
                String policyMEssage = "Could not get policies on the role because the arn role do not have enough permission: %s";
                LOGGER.info(String.format(policyMEssage, ase.getErrorMessage()));
                throw new CloudConnectorException(String.format(policyMEssage, ase.getErrorMessage()));
            } else {
                LOGGER.info(ase.getMessage());
                throw new CloudConnectorException(ase.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            throw new CloudConnectorException(e.getMessage());
        }
        LOGGER.info("Could not get policies on the role because the arn role do not have enough permission.");
        throw new CloudConnectorException("Could not get policies on the role because the arn role do not have enough permission.");
    }

    private boolean checkIamOrS3Statement(String roleName, AmazonIdentityManagement client, String s) throws UnsupportedEncodingException, JSONException {
        GetRolePolicyRequest getRolePolicyRequest = new GetRolePolicyRequest();
        getRolePolicyRequest.setRoleName(roleName);
        getRolePolicyRequest.setPolicyName(s);
        GetRolePolicyResult rolePolicy = client.getRolePolicy(getRolePolicyRequest);
        String decode = URLDecoder.decode(rolePolicy.getPolicyDocument(), "UTF-8");
        JSONObject object = new JSONObject(decode);
        JSONArray statement = object.getJSONArray("Statement");
        for (int i = 0; i < statement.length(); i++) {
            JSONArray action = statement.getJSONObject(i).getJSONArray("Action");
            for (int j = 0; j < action.length(); j++) {
                String actionEntry = action.get(j).toString().replaceAll(" ", "").toLowerCase();
                if ("iam:createrole".equals(actionEntry) || "iam:*".equals(actionEntry)) {
                    LOGGER.info("Role has able to operate on iam resources: {}.", action.get(j).toString());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkIamOrS3Access(AmazonIdentityManagement client, AttachedPolicy attachedPolicy) {
        GetPolicyRequest getRolePolicyRequest = new GetPolicyRequest();
        getRolePolicyRequest.setPolicyArn(attachedPolicy.getPolicyArn());
        GetPolicyResult policy = client.getPolicy(getRolePolicyRequest);
        if (policy.getPolicy().getArn().toLowerCase().contains("iam")) {
            LOGGER.info("Role has policy for iam resources: {}.", policy.getPolicy().getArn());
            return true;
        }
        return false;
    }

    private void validateExistingVpc(AwsNetworkView awsNetworkView, AmazonEC2Client amazonEC2Client) {
        DescribeVpcAttributeRequest describeVpcAttributeRequest = new DescribeVpcAttributeRequest();
        describeVpcAttributeRequest.withVpcId(awsNetworkView.getExistingVPC());
        boolean dnsSupported = isDnsSupported(amazonEC2Client, describeVpcAttributeRequest);
        boolean hostnameSupported = checkHostnameSupport(amazonEC2Client, describeVpcAttributeRequest);

        if (!dnsSupported || !hostnameSupported) {
            throw new CloudConnectorException("Please enable both DNS resolution and DNS hostnames in existing VPC: " + awsNetworkView.getExistingVPC());
        }
    }

    private boolean isDnsSupported(AmazonEC2Client amazonEC2Client, DescribeVpcAttributeRequest describeVpcAttributeRequest) {
        describeVpcAttributeRequest.withAttribute(VpcAttributeName.EnableDnsSupport);
        DescribeVpcAttributeResult describeVpcAttributeResult = amazonEC2Client.describeVpcAttribute(describeVpcAttributeRequest);
        return describeVpcAttributeResult.getEnableDnsSupport();
    }

    private boolean checkHostnameSupport(AmazonEC2Client amazonEC2Client, DescribeVpcAttributeRequest describeVpcAttributeRequest) {
        describeVpcAttributeRequest.withAttribute(VpcAttributeName.EnableDnsHostnames);
        DescribeVpcAttributeResult describeVpcAttributeResult = amazonEC2Client.describeVpcAttribute(describeVpcAttributeRequest);
        return describeVpcAttributeResult.getEnableDnsHostnames();
    }

    private void validateExistingSubnet(AwsNetworkView awsNetworkView, AmazonEC2Client amazonEC2Client) {
        if (awsNetworkView.isExistingSubnet()) {
            DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
            describeSubnetsRequest.withSubnetIds(awsNetworkView.getSubnetList());
            DescribeSubnetsResult describeSubnetsResult = amazonEC2Client.describeSubnets(describeSubnetsRequest);
            if (describeSubnetsResult.getSubnets().size() < awsNetworkView.getSubnetList().size()) {
                throw new CloudConnectorException(String.format(SUBNET_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingSubnet()));
            } else {
                for (Subnet subnet : describeSubnetsResult.getSubnets()) {
                    String vpcId = subnet.getVpcId();
                    if (vpcId != null && !vpcId.equals(awsNetworkView.getExistingVPC())) {
                        throw new CloudConnectorException(String.format(SUBNETVPC_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingSubnet(),
                                awsNetworkView.getExistingVPC()));
                    }
                }
            }
        }
    }

    private void validateExistingIGW(AwsNetworkView awsNetworkView, AmazonEC2Client amazonEC2Client) {
        if (awsNetworkView.isExistingIGW()) {
            DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest();
            describeInternetGatewaysRequest.withInternetGatewayIds(awsNetworkView.getExistingIGW());
            DescribeInternetGatewaysResult describeInternetGatewaysResult = amazonEC2Client.describeInternetGateways(describeInternetGatewaysRequest);
            if (describeInternetGatewaysResult.getInternetGateways().size() < 1) {
                throw new CloudConnectorException(String.format(IGW_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingIGW()));
            } else {
                InternetGateway internetGateway = describeInternetGatewaysResult.getInternetGateways().get(0);
                InternetGatewayAttachment attachment = internetGateway.getAttachments().get(0);
                if (attachment != null && !attachment.getVpcId().equals(awsNetworkView.getExistingVPC())) {
                    throw new CloudConnectorException(String.format(IGWVPC_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingIGW(),
                            awsNetworkView.getExistingVPC()));
                }
            }
        }
    }

    @Override
    public void validateFileSystem(FileSystem fileSystem) throws Exception {
    }

    private void validateExistingKeyPair(AuthenticatedContext authenticatedContext, AwsCredentialView credentialView, String region) {
        String keyPairName = awsClient.getExistingKeyPairName(authenticatedContext);
        if (StringUtils.isNoneEmpty(keyPairName)) {
            boolean keyPairIsPresentOnEC2 = false;
            try {
                AmazonEC2Client client = awsClient.createAccess(credentialView, region);
                DescribeKeyPairsResult describeKeyPairsResult = client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyPairName));
                keyPairIsPresentOnEC2 = describeKeyPairsResult.getKeyPairs().stream().findFirst().isPresent();
            } catch (Exception e) {
                String errorMessage = String.format("Failed to get the key pair [name: '%s'] from EC2 [roleArn:'%s'], detailed message: %s.",
                        keyPairName, credentialView.getRoleArn(), e.getMessage());
                LOGGER.error(errorMessage, e);
            }
            if (!keyPairIsPresentOnEC2) {
                throw new CloudConnectorException(String.format("The key pair '%s' could not be found in the '%s' region of EC2.", keyPairName, region));
            }
        }
    }
}
