package com.sequenceiq.cloudbreak.cloud.aws;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSshKeyFilterView;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceVpcFilterView;

@Service
public class AwsPlatformResources implements PlatformResources {

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Override
    public CloudNetworks networks(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudNetwork>> result = new HashMap<>();
        for (Region actualRegion : awsPlatformParameters.regions().types()) {
            // If region is provided then should filter for those region
            if (regionMatch(actualRegion, region)) {
                Set<CloudNetwork> cloudNetworks = new HashSet<>();
                AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), actualRegion.value());

                //create vpc filter view
                PlatformResourceVpcFilterView filter = new PlatformResourceVpcFilterView(filters);

                DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
                // If the filtervalue is provided then we should filter only for those vpc
                if (!Strings.isNullOrEmpty(filter.getVpcId())) {
                    describeVpcsRequest.withVpcIds(filter.getVpcId());
                }
                for (Vpc vpc : ec2Client.describeVpcs(describeVpcsRequest).getVpcs()) {
                    Set<String> subnetIds = ec2Client.describeSubnets(createVpcDescribeRequest(vpc))
                            .getSubnets().stream().map(Subnet::getSubnetId).collect(Collectors.toSet());
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("cidrBlock", vpc.getCidrBlock());
                    properties.put("default", vpc.getIsDefault());
                    properties.put("dhcpOptionsId", vpc.getDhcpOptionsId());
                    properties.put("instanceTenancy", vpc.getInstanceTenancy());
                    properties.put("state", vpc.getState());
                    cloudNetworks.add(new CloudNetwork(vpc.getVpcId(), subnetIds, properties));
                }
                result.put(actualRegion.value(), cloudNetworks);
            }
        }
        return new CloudNetworks(result);
    }

    private DescribeSubnetsRequest createVpcDescribeRequest(Vpc vpc) {
        return new DescribeSubnetsRequest().withFilters(new Filter("vpc-id", singletonList(vpc.getVpcId())));
    }

    @Override
    public CloudSshKeys sshKeys(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSshKey>> result = new HashMap<>();
        for (Region actualRegion : awsPlatformParameters.regions().types()) {
            // If region is provided then should filter for those region
            if (regionMatch(actualRegion, region)) {
                Set<CloudSshKey> cloudSshKeys = new HashSet<>();
                AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), actualRegion.value());

                //create sshkey filter view
                PlatformResourceSshKeyFilterView filter = new PlatformResourceSshKeyFilterView(filters);

                DescribeKeyPairsRequest describeKeyPairsRequest = new DescribeKeyPairsRequest();

                // If the filtervalue is provided then we should filter only for those securitygroups
                if (!Strings.isNullOrEmpty(filter.getKeyName())) {
                    describeKeyPairsRequest.withKeyNames(filter.getKeyName());
                }

                for (KeyPairInfo keyPairInfo : ec2Client.describeKeyPairs(describeKeyPairsRequest).getKeyPairs()) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("fingerPrint", keyPairInfo.getKeyFingerprint());
                    cloudSshKeys.add(new CloudSshKey(keyPairInfo.getKeyName(), properties));
                }
                result.put(actualRegion.value(), cloudSshKeys);
            }
        }
        return new CloudSshKeys(result);
    }

    @Override
    public CloudSecurityGroups securityGroups(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        Map<String, Set<CloudSecurityGroup>> result = new HashMap<>();
        for (Region actualRegion : awsPlatformParameters.regions().types()) {
            // If region is provided then should filter for those region
            if (regionMatch(actualRegion, region)) {
                Set<CloudSecurityGroup> cloudSecurityGroups = new HashSet<>();
                AmazonEC2Client ec2Client = awsClient.createAccess(new AwsCredentialView(cloudCredential), actualRegion.value());

                //create securitygroup filter view
                PlatformResourceSecurityGroupFilterView filter = new PlatformResourceSecurityGroupFilterView(filters);

                DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
                // If the filtervalue is provided then we should filter only for those securitygroups
                if (!Strings.isNullOrEmpty(filter.getVpcId())) {
                    describeSecurityGroupsRequest.withFilters(new Filter("vpc-id", singletonList(filter.getVpcId())));
                }
                if (!Strings.isNullOrEmpty(filter.getGroupId())) {
                    describeSecurityGroupsRequest.withGroupIds(filter.getGroupId());
                }
                if (!Strings.isNullOrEmpty(filter.getGroupName())) {
                    describeSecurityGroupsRequest.withGroupNames(filter.getGroupName());
                }

                for (SecurityGroup securityGroup : ec2Client.describeSecurityGroups(describeSecurityGroupsRequest).getSecurityGroups()) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("vpcId", securityGroup.getVpcId());
                    properties.put("description", securityGroup.getDescription());
                    properties.put("ipPermissions", securityGroup.getIpPermissions());
                    properties.put("ipPermissionsEgress", securityGroup.getIpPermissionsEgress());
                    cloudSecurityGroups.add(new CloudSecurityGroup(securityGroup.getGroupName(), securityGroup.getGroupId(), properties));
                }
                result.put(actualRegion.value(), cloudSecurityGroups);
            }
        }

        return new CloudSecurityGroups(result);
    }
}
