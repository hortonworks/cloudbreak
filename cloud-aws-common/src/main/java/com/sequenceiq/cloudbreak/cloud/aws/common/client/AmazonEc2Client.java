package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INCORRECT_INSTANCE_STATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INCORRECT_STATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INCORRECT_STATE_EXCEPTION;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_ERROR;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INVALID_HOST_STATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INVALID_STATE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.REQUEST_EXPIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVER_INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_UNAVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.UNAVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.VOLUME_IN_USE;

import java.util.Set;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.AttachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionResponse;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribePrefixListsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointServicesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DetachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputRequest;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputResponse;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceMetadataOptionsRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceMetadataOptionsResponse;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeResponse;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressResponse;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressResponse;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstanceTypeOfferingsIterable;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

public class AmazonEc2Client extends AmazonClient {

    private static final Set<String> RETRIABLE_ERRORS = Set.of(REQUEST_EXPIRED, INCORRECT_INSTANCE_STATE, INCORRECT_STATE, INCORRECT_STATE_EXCEPTION,
            INVALID_HOST_STATE, INVALID_STATE, VOLUME_IN_USE, SERVER_INTERNAL, INTERNAL_FAILURE, SERVICE_UNAVAILABLE, INTERNAL_ERROR, UNAVAILABLE);

    private final Ec2Client client;

    private final Retry retry;

    public AmazonEc2Client(Ec2Client client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateVolumeResponse createVolume(CreateVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.createVolume(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeSubnetsResponse describeSubnets(DescribeSubnetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeSubnets(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeNetworkInterfacesResponse describeNetworkInterfaces(DescribeNetworkInterfacesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeNetworkInterfaces(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttributeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.modifyInstanceAttribute(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DeleteVolumeResponse deleteVolume(DeleteVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.deleteVolume(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeVolumesResponse describeVolumes(DescribeVolumesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeVolumes(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public AttachVolumeResponse attachVolume(AttachVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.attachVolume(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeRegionsResponse describeRegions(DescribeRegionsRequest describeRegionsRequest) {
        return client.describeRegions(describeRegionsRequest);
    }

    public DescribeRouteTablesResponse describeRouteTables() {
        return client.describeRouteTables();
    }

    public DescribeVpcsResponse describeVpcs(DescribeVpcsRequest describeVpcsRequest) {
        return client.describeVpcs(describeVpcsRequest);
    }

    public DescribeVpcEndpointsResponse describeVpcEndpoints(DescribeVpcEndpointsRequest describeVpcEndpointsRequest) {
        return client.describeVpcEndpoints(describeVpcEndpointsRequest);
    }

    public DescribeKeyPairsResponse describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) {
        return client.describeKeyPairs(describeKeyPairsRequest);
    }

    public DescribeSecurityGroupsResponse describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        return client.describeSecurityGroups(describeSecurityGroupsRequest);
    }

    public DescribeAvailabilityZonesResponse describeAvailabilityZones(DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) {
        return client.describeAvailabilityZones(describeAvailabilityZonesRequest);
    }

    public DescribeInternetGatewaysResponse describeInternetGateways(DescribeInternetGatewaysRequest describeInternetGatewaysRequest) {
        return client.describeInternetGateways(describeInternetGatewaysRequest);
    }

    // FIXME return specific waiter instead
    public Ec2Waiter waiters() {
        return client.waiter();
    }

    public TerminateInstancesResponse terminateInstances(TerminateInstancesRequest terminateInstancesRequest) {
        return client.terminateInstances(terminateInstancesRequest);
    }

    public DescribeAddressesResponse describeAddresses(DescribeAddressesRequest describeAddressesRequest) {
        return client.describeAddresses(describeAddressesRequest);
    }

    public AssociateAddressResponse associateAddress(AssociateAddressRequest associateAddressRequest) {
        return client.associateAddress(associateAddressRequest);
    }

    public ImportKeyPairResponse importKeyPair(ImportKeyPairRequest importKeyPairRequest) {
        return client.importKeyPair(importKeyPairRequest);
    }

    public DescribePrefixListsResponse describePrefixLists() {
        return client.describePrefixLists();
    }

    public DescribeRouteTablesResponse describeRouteTables(DescribeRouteTablesRequest describeRouteTablesRequest) {
        return client.describeRouteTables(describeRouteTablesRequest);
    }

    public DescribeInstancesResponse retryableDescribeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return retry.testWith1SecDelayMax5TimesMaxDelay5MinutesMultiplier5(() -> {
            try {
                return client.describeInstances(describeInstancesRequest);
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND)) {
                    throw new ActionFailedException("The requested instances are not found on AWS side: " + describeInstancesRequest.instanceIds(), e);
                } else {
                    throw e;
                }
            }
        });
    }

    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return client.describeInstances(describeInstancesRequest);
    }

    public DescribeInstanceStatusResponse describeInstanceStatus(DescribeInstanceStatusRequest describeInstanceStatusRequest) {
        return client.describeInstanceStatus(describeInstanceStatusRequest);
    }

    public CreateTagsResponse createTags(CreateTagsRequest createTagsRequest) {
        return client.createTags(createTagsRequest);
    }

    public DescribeVpcEndpointServicesResponse describeVpcEndpointServices() {
        return client.describeVpcEndpointServices();
    }

    public DescribeAvailabilityZonesResponse describeAvailabilityZones() {
        return client.describeAvailabilityZones();
    }

    public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) {
        return client.deleteKeyPair(deleteKeyPairRequest);
    }

    public GetConsoleOutputResponse getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) {
        return client.getConsoleOutput(getConsoleOutputRequest);
    }

    public StopInstancesResponse stopInstances(StopInstancesRequest stopInstancesRequest) {
        return client.stopInstances(stopInstancesRequest);
    }

    public StartInstancesResponse startInstances(StartInstancesRequest startInstancesRequest) {
        return client.startInstances(startInstancesRequest);
    }

    public DescribeImagesResponse describeImages(DescribeImagesRequest describeImagesRequest) {
        return client.describeImages(describeImagesRequest);
    }

    public DescribeInstanceTypeOfferingsIterable describeInstanceTypeOfferings(DescribeInstanceTypeOfferingsRequest request) {
        return client.describeInstanceTypeOfferingsPaginator(request);
    }

    public DescribeInstanceTypesResponse describeInstanceTypes(DescribeInstanceTypesRequest request) {
        return client.describeInstanceTypes(request);
    }

    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {
        return client.createSecurityGroup(request);
    }

    public AuthorizeSecurityGroupIngressResponse addIngress(AuthorizeSecurityGroupIngressRequest request) {
        return client.authorizeSecurityGroupIngress(request);
    }

    public AuthorizeSecurityGroupEgressResponse addEgress(AuthorizeSecurityGroupEgressRequest request) {
        return client.authorizeSecurityGroupEgress(request);
    }

    public RevokeSecurityGroupEgressResponse revokeEgress(RevokeSecurityGroupEgressRequest request) {
        return client.revokeSecurityGroupEgress(request);
    }

    public AllocateAddressResponse allocateAddress(AllocateAddressRequest request) {
        return client.allocateAddress(request);
    }

    public ReleaseAddressResponse releaseAddress(ReleaseAddressRequest request) {
        return client.releaseAddress(request);
    }

    public DisassociateAddressResponse disassociateAddress(DisassociateAddressRequest request) {
        return client.disassociateAddress(request);
    }

    public RunInstancesResponse createInstance(RunInstancesRequest request) {
        return client.runInstances(request);
    }

    public TerminateInstancesResponse deleteInstance(TerminateInstancesRequest request) {
        return client.terminateInstances(request);
    }

    public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroupRequest request) {
        return client.deleteSecurityGroup(request);
    }

    public CreateLaunchTemplateVersionResponse createLaunchTemplateVersion(CreateLaunchTemplateVersionRequest request) {
        return client.createLaunchTemplateVersion(request);
    }

    public ModifyLaunchTemplateResponse modifyLaunchTemplate(ModifyLaunchTemplateRequest request) {
        return client.modifyLaunchTemplate(request);
    }

    public DescribeLaunchTemplateVersionsResponse describeLaunchTemplateVersions(DescribeLaunchTemplateVersionsRequest request) {
        return client.describeLaunchTemplateVersions(request);
    }

    public DetachVolumeResponse detachVolume(DetachVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.detachVolume(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public ModifyVolumeResponse modifyVolume(ModifyVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.modifyVolume(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeVolumesModificationsResponse describeVolumeModifications(DescribeVolumesModificationsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeVolumesModifications(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public ModifyInstanceMetadataOptionsResponse modifyInstanceMetadataOptions(ModifyInstanceMetadataOptionsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.modifyInstanceMetadataOptions(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    private RuntimeException createActionFailedExceptionIfRetriableError(AwsServiceException ex) {
        if (ex.awsErrorDetails() != null) {
            String errorCode = ex.awsErrorDetails().errorCode();
            if (RETRIABLE_ERRORS.contains(errorCode)) {
                return new ActionFailedException(ex);
            }
        }
        return ex;
    }

}