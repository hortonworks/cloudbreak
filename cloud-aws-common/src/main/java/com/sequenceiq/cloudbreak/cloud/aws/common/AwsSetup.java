package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.Subnet;

public abstract class AwsSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSetup.class);

    private static final String IGW_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not exist or belongs to a different region.";

    private static final String SUBNET_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not exist or belongs to a different region.";

    private static final String SUBNETVPC_DOES_NOT_EXIST_MSG = "The given subnet '%s' does not belong to the given VPC '%s'.";

    private static final String IGWVPC_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not belong to the given VPC '%s'.";

    private static final int FINISHED_PROGRESS_VALUE = 100;

    @Value("${cb.aws.spotinstances.enabled:}")
    private boolean awsSpotinstanceEnabled;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, Image image) {
        return new ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE);
    }

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, Image image, PrepareImageType prepareImageType,
            String fallbackTargetImage) {
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public void validateImage(AuthenticatedContext auth, CloudStack stack, Image image) {
        LOGGER.debug("validate image has been executed");
    }

    @Override
    public void prerequisites(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        verifySpotInstances(stack);
        if (awsNetworkView.isExistingVPC()) {
            try {
                AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(ac).getAmazonEC2Client();
                validateExistingIGW(awsNetworkView, amazonEC2Client);
                validateExistingSubnet(awsNetworkView, amazonEC2Client);
            } catch (AwsServiceException e) {
                throw new CloudConnectorException(e.awsErrorDetails().errorMessage());
            } catch (SdkClientException e) {
                throw new CloudConnectorException(e.getMessage());
            }

        }
        validateRegionAndZone(ac.getCloudCredential(), ac.getCloudContext().getLocation());
        validateExistingKeyPair(stack.getInstanceAuthentication(), credentialView, region, ac);
        LOGGER.debug("setup has been executed");
    }

    private void validateRegionAndZone(CloudCredential cloudCredential, Location location) {
        List<String> entitlements = entitlementService.getEntitlements(cloudCredential.getAccountId());
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(
                cloudCredential,
                CloudPlatform.AWS.name(),
                "",
                cloudCredential.getAccountId(),
                entitlements);
        CloudRegions regions = awsPlatformResources.regions(extendedCloudCredential, location.getRegion(), Collections.emptyMap(), true);
        List<AvailabilityZone> availabilityZones = regions.getCloudRegions().get(location.getRegion());
        if (availabilityZones == null) {
            throw new CloudConnectorException(String.format("Region [%s] doesn't contain any availability zone",
                    location.getRegion().getRegionName()));
        }
        if (location.getAvailabilityZone() != null
                && !availabilityZones.contains(location.getAvailabilityZone())) {
            throw new CloudConnectorException(String.format("Region [%s] doesn't contain availability zone [%s]",
                    location.getRegion().getRegionName(), location.getAvailabilityZone().value()));
        }
    }

    private void verifySpotInstances(CloudStack stack) {
        if (!awsSpotinstanceEnabled) {
            for (Group group : stack.getGroups()) {
                if (group.getInstances() != null
                        && !group.getInstances().isEmpty()
                        && new AwsInstanceView(group.getReferenceInstanceTemplate()).getSpotPercentage() != null) {
                    throw new CloudConnectorException(String.format("Spot instances are not supported on this AMI: %s", stack.getImage()));
                }
            }
        }
    }

    private void validateExistingSubnet(AwsNetworkView awsNetworkView, AmazonEc2Client amazonEC2Client) {
        if (awsNetworkView.isExistingSubnet()) {
            DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder()
                    .subnetIds(awsNetworkView.getSubnetList())
                    .build();
            DescribeSubnetsResponse describeSubnetsResponse = amazonEC2Client.describeSubnets(describeSubnetsRequest);
            if (describeSubnetsResponse.subnets().size() < awsNetworkView.getSubnetList().size()) {
                throw new CloudConnectorException(String.format(SUBNET_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingSubnet()));
            } else {
                for (Subnet subnet : describeSubnetsResponse.subnets()) {
                    String vpcId = subnet.vpcId();
                    if (vpcId != null && !vpcId.equals(awsNetworkView.getExistingVpc())) {
                        throw new CloudConnectorException(String.format(SUBNETVPC_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingSubnet(),
                                awsNetworkView.getExistingVpc()));
                    }
                }
            }
        }
    }

    private void validateExistingIGW(AwsNetworkView awsNetworkView, AmazonEc2Client amazonEC2Client) {
        if (awsNetworkView.isExistingIGW()) {
            DescribeInternetGatewaysRequest describeInternetGatewaysRequest = DescribeInternetGatewaysRequest.builder()
                    .internetGatewayIds(awsNetworkView.getExistingIgw())
                    .build();
            DescribeInternetGatewaysResponse describeInternetGatewaysResponse = amazonEC2Client.describeInternetGateways(describeInternetGatewaysRequest);
            if (describeInternetGatewaysResponse.internetGateways().size() < 1) {
                throw new CloudConnectorException(String.format(IGW_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingIgw()));
            } else {
                InternetGateway internetGateway = describeInternetGatewaysResponse.internetGateways().get(0);
                InternetGatewayAttachment attachment = internetGateway.attachments().get(0);
                if (attachment != null && !attachment.vpcId().equals(awsNetworkView.getExistingVpc())) {
                    throw new CloudConnectorException(String.format(IGWVPC_DOES_NOT_EXIST_MSG, awsNetworkView.getExistingIgw(),
                            awsNetworkView.getExistingVpc()));
                }
            }
        }
    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) {

    }

    @Override
    public void validateParameters(AuthenticatedContext authenticatedContext, Map<String, String> parameters) {

    }

    private void validateExistingKeyPair(InstanceAuthentication instanceAuthentication, AwsCredentialView credentialView, String region,
            AuthenticatedContext ac) {
        String keyPairName = awsClient.getExistingKeyPairName(instanceAuthentication);
        if (StringUtils.isNotEmpty(keyPairName)) {
            boolean keyPairIsPresentOnEC2 = false;
            try {
                AmazonEc2Client client = new AuthenticatedContextView(ac).getAmazonEC2Client();
                DescribeKeyPairsResponse describeKeyPairsResponse = client.describeKeyPairs(DescribeKeyPairsRequest.builder()
                        .keyNames(keyPairName)
                        .build());
                keyPairIsPresentOnEC2 = describeKeyPairsResponse.keyPairs().stream().findFirst().isPresent();
            } catch (RuntimeException e) {
                String errorMessage = String.format("Failed to get the key pair [name: '%s'] from EC2 [roleArn:'%s'], detailed message: %s.",
                        keyPairName, credentialView.getRoleArn(), e.getMessage());
                LOGGER.info(errorMessage, e);
            }
            if (!keyPairIsPresentOnEC2) {
                throw new CloudConnectorException(
                        String.format("The key pair '%s' could not be found in the '%s' region of EC2. " +
                                        "Please check AWS EC2 console because probably you are using a wrong key or " +
                                        "refer to Cloudera documentation at %s for the required setup",
                                keyPairName,
                                region,
                                DocumentationLinkProvider.awsSshKeySetupLink()));
            }
        }
    }

}
