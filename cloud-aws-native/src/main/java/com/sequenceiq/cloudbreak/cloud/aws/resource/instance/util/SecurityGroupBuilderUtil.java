package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.PrefixListId;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class SecurityGroupBuilderUtil {

    public static final int TO_PORT = 65535;

    private static final Logger LOGGER = getLogger(SecurityGroupBuilderUtil.class);

    @Inject
    private AwsResourceNameService awsResourceNameService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsNetworkService awsNetworkService;

    public String createSecurityGroup(Network network, Group group, AmazonEc2Client amazonEc2Client,
            CloudContext context, AuthenticatedContext ac) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest()
                .withDescription("Allow access from web and bastion as well as outbound HTTP and HTTPS traffic")
                .withVpcId(awsNetworkView.getExistingVpc())
                .withGroupName(awsResourceNameService.resourceName(ResourceType.AWS_SECURITY_GROUP, context.getName(), group.getName(), context.getId()))
                .withTagSpecifications(awsTaggingService.prepareEc2TagSpecification(group.getTags(),
                        com.amazonaws.services.ec2.model.ResourceType.SecurityGroup));
        return createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
    }

    public String createOrGetSecurityGroup(AmazonEc2Client amazonEc2Client, CreateSecurityGroupRequest request, Group group,
            AwsNetworkView awsNetworkView, AuthenticatedContext ac) {
        String securityGroupId;
        try {
            CreateSecurityGroupResult securityGroup = amazonEc2Client.createSecurityGroup(request);
            LOGGER.info("Security group created successfully for group: {} and vpc: {}", request.getGroupName(), request.getVpcId());
            ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroup.getGroupId());
            egress(amazonEc2Client, ac, awsNetworkView, securityGroup.getGroupId(), Collections.emptyList());
            securityGroupId = securityGroup.getGroupId();
        } catch (AmazonEC2Exception e) {
            if (!e.getErrorCode().equals("InvalidGroup.Duplicate")) {
                throw e;
            }
            LOGGER.debug("Security group exists with name of {} for {}, try to fetch it", request.getGroupName(), request.getVpcId());
            SecurityGroup securityGroup = getSecurityGroup(amazonEc2Client, request.getVpcId(), request.getGroupName());
            String groupId = securityGroup.getGroupId();
            if (securityGroup.getIpPermissions().isEmpty()) {
                ingress(group, ac, amazonEc2Client, awsNetworkView, groupId);
            }
            egress(amazonEc2Client, ac, awsNetworkView, securityGroup.getGroupId(), securityGroup.getIpPermissionsEgress());
            securityGroupId = groupId;
        }
        return securityGroupId;
    }

    public SecurityGroup getSecurityGroup(AmazonEc2Client amazonEc2Client, String vpcId, String groupName) {
        Optional<SecurityGroup> securityGroupOpt = describeSecurityGroup(amazonEc2Client, vpcId, groupName);
        if (securityGroupOpt.isEmpty()) {
            LOGGER.debug("Security group is not exist on the provider with {} groupName in the vpc: {}.", groupName, vpcId);
            throw NotFoundException.notFoundException("Aws Security Group", groupName);
        }
        LOGGER.debug("Security group fetched from aws by vpc id: {} and group name: {}", vpcId, groupName);
        return securityGroupOpt.get();
    }

    public SecurityGroup getSecurityGroupSilent(AmazonEc2Client amazonEc2Client, String vpcId, String groupName) {
        Optional<SecurityGroup> securityGroupOpt = describeSecurityGroup(amazonEc2Client, vpcId, groupName);
        if (securityGroupOpt.isEmpty()) {
            LOGGER.debug("Security group is not exist on the provider with {} groupName in the vpc: {}. Does not need exception", groupName, vpcId);
            return null;
        }
        LOGGER.debug("Security group fetched from aws by vpc id: {} and group name: {}", vpcId, groupName);
        return securityGroupOpt.get();
    }

    private Optional<SecurityGroup> describeSecurityGroup(AmazonEc2Client amazonEc2Client, String vpcId, String groupName) {
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest()
                .withFilters(new Filter().withName("vpc-id").withValues(vpcId));
        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEc2Client.describeSecurityGroups(describeSecurityGroupsRequest);
        return describeSecurityGroupsResult.getSecurityGroups().stream()
                .filter(result -> result.getGroupName().equals(groupName)).findFirst();
    }

    public void ingress(Group group, AuthenticatedContext ac, AmazonEc2Client amazonEc2Client, AwsNetworkView awsNetworkView, String securityGroupId) {
        Set<IpPermission> permissions = new HashSet<>();
        for (SecurityRule rule : group.getSecurity().getRules()) {
            for (PortDefinition port : rule.getPorts()) {
                permissions.add(new IpPermission()
                        .withIpProtocol(rule.getProtocol())
                        .withFromPort(Integer.parseInt(port.getFrom()))
                        .withToPort(Integer.parseInt(port.getTo()))
                        .withIpv4Ranges(new IpRange().withCidrIp(rule.getCidr())));
            }
        }
        for (String cidr : awsNetworkService.getVpcCidrs(ac, awsNetworkView)) {
            permissions.add(new IpPermission()
                    .withIpProtocol("icmp")
                    .withFromPort(-1)
                    .withToPort(-1)
                    .withIpv4Ranges(new IpRange().withCidrIp(cidr)));
            permissions.add(new IpPermission()
                    .withIpProtocol("tcp")
                    .withFromPort(0)
                    .withToPort(TO_PORT)
                    .withIpv4Ranges(new IpRange().withCidrIp(cidr)));
            permissions.add(new IpPermission()
                    .withIpProtocol("udp")
                    .withFromPort(0)
                    .withToPort(TO_PORT)
                    .withIpv4Ranges(new IpRange().withCidrIp(cidr)));
        }
        AuthorizeSecurityGroupIngressRequest reguest = new AuthorizeSecurityGroupIngressRequest()
                .withGroupId(securityGroupId)
                .withIpPermissions(permissions);
        amazonEc2Client.addIngress(reguest);
        LOGGER.info("Ingress added to {}", securityGroupId);
    }

    public void egress(AmazonEc2Client amazonEc2Client, AuthenticatedContext ac, AwsNetworkView awsNetworkView, String securityGroupId,
            List<IpPermission> egress) {
        OutboundInternetTraffic outboundInternetTraffic = awsNetworkView.getOutboundInternetTraffic();
        List<String> prefixListIds = awsNetworkService.getPrefixListIds(amazonEc2Client, ac.getCloudContext().getLocation().getRegion().getRegionName(),
                outboundInternetTraffic);
        List<String> vpcCidrs = awsNetworkService.getVpcCidrs(ac, awsNetworkView);
        if (outboundInternetTraffic == OutboundInternetTraffic.DISABLED && (!prefixListIds.isEmpty() || !vpcCidrs.isEmpty())) {
            List<IpPermission> permissions = new ArrayList<>();
            for (String existingVpcCidr : vpcCidrs) {
                IpPermission e = new IpPermission().withIpProtocol("-1").withIpv4Ranges(new IpRange().withCidrIp(existingVpcCidr));
                if (!egress.contains(e)) {
                    permissions.add(e);
                }
            }
            for (String prefixListId : prefixListIds) {
                IpPermission e = new IpPermission()
                        .withIpProtocol("-1")
                        .withFromPort(0)
                        .withToPort(TO_PORT)
                        .withPrefixListIds(new PrefixListId().withPrefixListId(prefixListId));
                if (!egress.contains(e)) {
                    permissions.add(e);
                }
            }
            if (!permissions.isEmpty()) {
                AuthorizeSecurityGroupEgressRequest reguest = new AuthorizeSecurityGroupEgressRequest()
                        .withGroupId(securityGroupId)
                        .withIpPermissions(permissions);
                amazonEc2Client.addEgress(reguest);
                LOGGER.info("Egress added to {}", securityGroupId);
            } else {
                LOGGER.debug("No permission for egress request, skip it");
            }
        } else {
            LOGGER.debug("Egress creation skipped: {}, prefix list size: {}, vpc cidrs size: {}",
                    outboundInternetTraffic, prefixListIds.size(), vpcCidrs.size());
        }
    }

    public List<String> getSecurityGroupIds(AwsContext context, Group group) {
        List<CloudResource> groupResources = context.getGroupResources(group.getName());
        List<String> securityGroupIds = null;
        if (groupResources != null) {
            securityGroupIds = groupResources.stream()
                    .filter(g -> g.getType() == ResourceType.AWS_SECURITY_GROUP && group.getName().equals(g.getGroup()))
                    .map(CloudResource::getReference)
                    .collect(Collectors.toList());
            LOGGER.debug("Selected security group IDs from CloudResource: {}", securityGroupIds);
        }
        if (CollectionUtils.isEmpty(securityGroupIds)) {
            securityGroupIds = group.getSecurity().getCloudSecurityIds();
            LOGGER.debug("Selected security group ID from group's security domain: {}", securityGroupIds);
        }
        if (CollectionUtils.isEmpty(securityGroupIds)) {
            LOGGER.debug("Cannot determine the security group, so it will be created in the default security group");
        }
        LOGGER.info("Security group IDs: {}", securityGroupIds);
        return securityGroupIds;
    }
}
