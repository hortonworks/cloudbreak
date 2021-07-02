package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsResult;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribePrefixListsResult;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DescribeVpcEndpointServicesResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairResult;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonEc2Client extends AmazonClient {

    private final AmazonEC2 client;

    private final Retry retry;

    public AmazonEc2Client(AmazonEC2 client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateVolumeResult createVolume(CreateVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.createVolume(request));
    }

    public DescribeSubnetsResult describeSubnets(DescribeSubnetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeSubnets(request));
    }

    public ModifyInstanceAttributeResult modifyInstanceAttribute(ModifyInstanceAttributeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.modifyInstanceAttribute(request));
    }

    public DeleteVolumeResult deleteVolume(DeleteVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.deleteVolume(request));
    }

    public DescribeVolumesResult describeVolumes(DescribeVolumesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeVolumes(request));
    }

    public AttachVolumeResult attachVolume(AttachVolumeRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.attachVolume(request));
    }

    public DescribeRegionsResult describeRegions(DescribeRegionsRequest describeRegionsRequest) {
        return client.describeRegions(describeRegionsRequest);
    }

    public DescribeRouteTablesResult describeRouteTables() {
        return client.describeRouteTables();
    }

    public DescribeVpcsResult describeVpcs(DescribeVpcsRequest describeVpcsRequest) {
        return client.describeVpcs(describeVpcsRequest);
    }

    public DescribeKeyPairsResult describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) {
        return client.describeKeyPairs(describeKeyPairsRequest);
    }

    public DescribeSecurityGroupsResult describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {
        return client.describeSecurityGroups(describeSecurityGroupsRequest);
    }

    public DescribeAvailabilityZonesResult describeAvailabilityZones(DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) {
        return client.describeAvailabilityZones(describeAvailabilityZonesRequest);
    }

    public DescribeInternetGatewaysResult describeInternetGateways(DescribeInternetGatewaysRequest describeInternetGatewaysRequest) {
        return client.describeInternetGateways(describeInternetGatewaysRequest);
    }

    // FIXME return specific waiter instead
    public AmazonEC2Waiters waiters() {
        return client.waiters();
    }

    public TerminateInstancesResult terminateInstances(TerminateInstancesRequest terminateInstancesRequest) {
        return client.terminateInstances(terminateInstancesRequest);
    }

    public DescribeAddressesResult describeAddresses(DescribeAddressesRequest describeAddressesRequest) {
        return client.describeAddresses(describeAddressesRequest);
    }

    public AssociateAddressResult associateAddress(AssociateAddressRequest associateAddressRequest) {
        return client.associateAddress(associateAddressRequest);
    }

    public ImportKeyPairResult importKeyPair(ImportKeyPairRequest importKeyPairRequest) {
        return client.importKeyPair(importKeyPairRequest);
    }

    public DescribePrefixListsResult describePrefixLists() {
        return client.describePrefixLists();
    }

    public DescribeRouteTablesResult describeRouteTables(DescribeRouteTablesRequest describeRouteTablesRequest) {
        return client.describeRouteTables(describeRouteTablesRequest);
    }

    public DescribeInstancesResult describeInstances(DescribeInstancesRequest describeInstancesRequest) {
        return client.describeInstances(describeInstancesRequest);
    }

    public CreateTagsResult createTags(CreateTagsRequest createTagsRequest) {
        return client.createTags(createTagsRequest);
    }

    public DescribeVpcEndpointServicesResult describeVpcEndpointServices() {
        return client.describeVpcEndpointServices();
    }

    public DescribeAvailabilityZonesResult describeAvailabilityZones() {
        return client.describeAvailabilityZones();
    }

    public DeleteKeyPairResult deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) {
        return client.deleteKeyPair(deleteKeyPairRequest);
    }

    public GetConsoleOutputResult getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) {
        return client.getConsoleOutput(getConsoleOutputRequest);
    }

    public StopInstancesResult stopInstances(StopInstancesRequest stopInstancesRequest) {
        return client.stopInstances(stopInstancesRequest);
    }

    public StartInstancesResult startInstances(StartInstancesRequest startInstancesRequest) {
        return client.startInstances(startInstancesRequest);
    }

    public DescribeImagesResult describeImages(DescribeImagesRequest describeImagesRequest) {
        return client.describeImages(describeImagesRequest);
    }

    public DescribeInstanceTypeOfferingsResult describeInstanceTypeOfferings(DescribeInstanceTypeOfferingsRequest request) {
        return client.describeInstanceTypeOfferings(request);
    }

    public DescribeInstanceTypesResult describeInstanceTypes(DescribeInstanceTypesRequest request) {
        return client.describeInstanceTypes(request);
    }

    public CreateSecurityGroupResult createSecurityGroup(CreateSecurityGroupRequest request) {
        return client.createSecurityGroup(request);
    }

    public AuthorizeSecurityGroupIngressResult addIngress(AuthorizeSecurityGroupIngressRequest reguest) {
        return client.authorizeSecurityGroupIngress(reguest);
    }

    public AuthorizeSecurityGroupEgressResult addEgress(AuthorizeSecurityGroupEgressRequest reguest) {
        return client.authorizeSecurityGroupEgress(reguest);
    }

    // WIP
    public CreateVpcResult createVpc(Network network, Security security) {
        CreateVpcRequest request = new CreateVpcRequest();
        request.withCidrBlock(security.getCloudSecurityId());
        return client.createVpc(request);
    }

    public RunInstancesResult createInstance(RunInstancesRequest request) {
        return client.runInstances(request);
    }

    public TerminateInstancesResult deleteInstance(TerminateInstancesRequest request) {
        return client.terminateInstances(request);
    }

    public DeleteSecurityGroupResult deleteSecurityGroup(DeleteSecurityGroupRequest request) {
        return client.deleteSecurityGroup(request);
    }
}
