package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;

import com.sequenceiq.cloudbreak.service.Retry;

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
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.DescribePrefixListsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcEndpointServicesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputRequest;
import software.amazon.awssdk.services.ec2.model.GetConsoleOutputResponse;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateRequest;
import software.amazon.awssdk.services.ec2.model.ModifyLaunchTemplateResponse;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressResponse;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

public class AmazonEc2Client extends AmazonClient {

    private final Ec2Client client;

    private final Retry retry;

    public AmazonEc2Client(Ec2Client client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateVolumeResponse createVolume(CreateVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.createVolume(request));
    }

    public DescribeSubnetsResponse describeSubnets(DescribeSubnetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeSubnets(request));
    }

    public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttributeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.modifyInstanceAttribute(request));
    }

    public DeleteVolumeResponse deleteVolume(DeleteVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteVolume(request));
    }

    public DescribeVolumesResponse describeVolumes(DescribeVolumesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeVolumes(request));
    }

    public AttachVolumeResponse attachVolume(AttachVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.attachVolume(request));
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
                if (e.awsErrorDetails().errorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND_ERROR_CODE)) {
                    throw new Retry.ActionFailedException("The requested instances are not found on AWS side: " + describeInstancesRequest.instanceIds(), e);
                } else {
                    throw e;
                }
            }
        });
    }

    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return client.describeInstances(describeInstancesRequest);
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

    public DescribeInstanceTypeOfferingsResponse describeInstanceTypeOfferings(DescribeInstanceTypeOfferingsRequest request) {
        return client.describeInstanceTypeOfferings(request);
    }

    public DescribeInstanceTypesResponse describeInstanceTypes(DescribeInstanceTypesRequest request) {
        return client.describeInstanceTypes(request);
    }

    public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroupRequest request) {
        return client.createSecurityGroup(request);
    }

    public AuthorizeSecurityGroupIngressResponse addIngress(AuthorizeSecurityGroupIngressRequest reguest) {
        return client.authorizeSecurityGroupIngress(reguest);
    }

    public AuthorizeSecurityGroupEgressResponse addEgress(AuthorizeSecurityGroupEgressRequest reguest) {
        return client.authorizeSecurityGroupEgress(reguest);
    }

    public AllocateAddressResponse allocateAddress(AllocateAddressRequest reguest) {
        return client.allocateAddress(reguest);
    }

    public ReleaseAddressResponse releaseAddress(ReleaseAddressRequest reguest) {
        return client.releaseAddress(reguest);
    }

    public DisassociateAddressResponse disassociateAddress(DisassociateAddressRequest reguest) {
        return client.disassociateAddress(reguest);
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
}
