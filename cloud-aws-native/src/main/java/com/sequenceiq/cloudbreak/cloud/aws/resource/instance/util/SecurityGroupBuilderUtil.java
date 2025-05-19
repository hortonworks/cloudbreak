package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.GROUP_DUPLICATE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

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

import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.PrefixListId;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

@Component
public class SecurityGroupBuilderUtil {

    public static final int TO_PORT = 65535;

    private static final Logger LOGGER = getLogger(SecurityGroupBuilderUtil.class);

    private static final String AWS_CLIENT_DUPLICATE_ERROR_CODE = "InvalidPermission.Duplicate";

    private static final String AWS_CLIENT_NOT_FOUND_ERROR_CODE = "InvalidPermission.NotFound";

    @Inject
    private AwsResourceNameService awsResourceNameService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsNetworkService awsNetworkService;

    public String createSecurityGroup(Network network, Group group, AmazonEc2Client amazonEc2Client,
            CloudContext context, AuthenticatedContext ac) {
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .description("Allow access from web and bastion as well as outbound HTTP and HTTPS traffic")
                .vpcId(awsNetworkView.getExistingVpc())
                .groupName(awsResourceNameService.securityGroup(context.getName(), group.getName(), context.getId()))
                .tagSpecifications(awsTaggingService.prepareEc2TagSpecification(group.getTags(),
                        software.amazon.awssdk.services.ec2.model.ResourceType.SECURITY_GROUP))
                .build();
        return createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
    }

    public String createOrGetSecurityGroup(AmazonEc2Client amazonEc2Client, CreateSecurityGroupRequest request, Group group,
            AwsNetworkView awsNetworkView, AuthenticatedContext ac) {
        String securityGroupId;
        try {
            CreateSecurityGroupResponse securityGroup = amazonEc2Client.createSecurityGroup(request);
            LOGGER.info("Security group created successfully for group: {} and vpc: {}", request.groupName(), request.vpcId());
            ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroup.groupId());
            egress(amazonEc2Client, ac, awsNetworkView, securityGroup.groupId(), Collections.emptyList());
            securityGroupId = securityGroup.groupId();
        } catch (Ec2Exception e) {
            if (!GROUP_DUPLICATE.equals(e.awsErrorDetails().errorCode())) {
                throw e;
            }
            LOGGER.debug("Security group exists with name of {} for {}, try to fetch it", request.groupName(), request.vpcId());
            SecurityGroup securityGroup = getSecurityGroup(amazonEc2Client, request.vpcId(), request.groupName());
            String groupId = securityGroup.groupId();
            if (securityGroup.ipPermissions().isEmpty()) {
                ingress(group, ac, amazonEc2Client, awsNetworkView, groupId);
            }
            egress(amazonEc2Client, ac, awsNetworkView, securityGroup.groupId(), securityGroup.ipPermissionsEgress());
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
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = DescribeSecurityGroupsRequest.builder()
                .filters(Filter.builder().name("vpc-id").values(vpcId).build()).build();
        DescribeSecurityGroupsResponse describeSecurityGroupsResponse = amazonEc2Client.describeSecurityGroups(describeSecurityGroupsRequest);
        return describeSecurityGroupsResponse.securityGroups().stream()
                .filter(response -> response.groupName().equals(groupName)).findFirst();
    }

    public void ingress(Group group, AuthenticatedContext ac, AmazonEc2Client amazonEc2Client, AwsNetworkView awsNetworkView, String securityGroupId) {
        Set<IpPermission> permissions = new HashSet<>();
        for (SecurityRule rule : group.getSecurity().getRules()) {
            for (PortDefinition port : rule.getPorts()) {
                permissions.add(IpPermission.builder()
                        .ipProtocol(rule.getProtocol())
                        .fromPort(Integer.parseInt(port.getFrom()))
                        .toPort(Integer.parseInt(port.getTo()))
                        .ipRanges(IpRange.builder().cidrIp(rule.getCidr()).build())
                        .build());
            }
        }
        for (String cidr : awsNetworkService.getVpcCidrs(ac, awsNetworkView)) {
            permissions.add(IpPermission.builder()
                    .ipProtocol("icmp")
                    .fromPort(-1)
                    .toPort(-1)
                    .ipRanges(IpRange.builder().cidrIp(cidr).build())
                    .build());
            permissions.add(IpPermission.builder()
                    .ipProtocol("tcp")
                    .fromPort(0)
                    .toPort(TO_PORT)
                    .ipRanges(IpRange.builder().cidrIp(cidr).build())
                    .build());
            permissions.add(IpPermission.builder()
                    .ipProtocol("udp")
                    .fromPort(0)
                    .toPort(TO_PORT)
                    .ipRanges(IpRange.builder().cidrIp(cidr).build())
                    .build());
        }
        if (!permissions.isEmpty()) {
            LOGGER.debug("Adding ingress rules to security group ({}): {}", securityGroupId, permissions);
            AuthorizeSecurityGroupIngressRequest reguest = AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(securityGroupId)
                    .ipPermissions(permissions)
                    .build();
            amazonEc2Client.addIngress(reguest);
            LOGGER.info("Ingress added to {}", securityGroupId);
        } else {
            LOGGER.info("No ingress rule has been generated, skipping the addition of ingress rules");
        }
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
                IpPermission e = IpPermission.builder().ipProtocol("-1").ipRanges(IpRange.builder().cidrIp(existingVpcCidr).build()).build();
                if (!egress.contains(e)) {
                    permissions.add(e);
                }
            }
            for (String prefixListId : prefixListIds) {
                IpPermission e = IpPermission.builder()
                        .ipProtocol("-1")
                        .fromPort(0)
                        .toPort(TO_PORT)
                        .prefixListIds(PrefixListId.builder().prefixListId(prefixListId).build())
                        .build();
                if (!egress.contains(e)) {
                    permissions.add(e);
                }
            }
            if (!permissions.isEmpty()) {
                addEgressRules(amazonEc2Client, securityGroupId, permissions);
                revokeDefaultOutboundEgressRule(amazonEc2Client, securityGroupId);
            } else {
                LOGGER.debug("No permission for egress request, skip it");
            }
        } else {
            LOGGER.debug("Egress creation skipped: {}, prefix list size: {}, vpc cidrs size: {}",
                    outboundInternetTraffic, prefixListIds.size(), vpcCidrs.size());
        }
    }

    private void addEgressRules(AmazonEc2Client amazonEc2Client, String securityGroupId, List<IpPermission> permissions) {
        LOGGER.debug("Adding egress rules to security group ({}): {}", securityGroupId, permissions);
        AuthorizeSecurityGroupEgressRequest request = AuthorizeSecurityGroupEgressRequest.builder()
                .groupId(securityGroupId)
                .ipPermissions(permissions)
                .build();
        try {
            amazonEc2Client.addEgress(request);
            LOGGER.info("Egress added to {}", securityGroupId);
        } catch (Ec2Exception ex) {
            if (AWS_CLIENT_DUPLICATE_ERROR_CODE.equals(ex.awsErrorDetails().errorCode())) {
                LOGGER.info("Security egress already exists for security group: '{}' with request: {}", securityGroupId, request);
            } else {
                LOGGER.error("Failed to create security egress for security group: '{}' with request: {}", securityGroupId, request, ex);
                throw ex;
            }
        }
    }

    private void revokeDefaultOutboundEgressRule(AmazonEc2Client amazonEc2Client, String securityGroupId) {
        RevokeSecurityGroupEgressRequest revokeRequest = RevokeSecurityGroupEgressRequest.builder()
                .groupId(securityGroupId)
                .ipPermissions(IpPermission.builder()
                        .ipProtocol("-1")
                        .fromPort(0)
                        .toPort(0)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                        .build())
                .build();
        try {
            LOGGER.debug("Adding default security egress rule to security group: '{}' with request: {}", securityGroupId, revokeRequest);
            amazonEc2Client.revokeEgress(revokeRequest);
            LOGGER.info("Default allow all outbound traffic egress rule has been revoked for security group ({}).", securityGroupId);
        } catch (Ec2Exception ex) {
            if (AWS_CLIENT_NOT_FOUND_ERROR_CODE.equals(ex.awsErrorDetails().errorCode())) {
                LOGGER.info("Default security egress rule has already been revoked on security group: '{}' failed request: {}", securityGroupId, revokeRequest);
            } else {
                LOGGER.error("Failed to revoke the default security egress rule for security group: '{}' with request: {}", securityGroupId, revokeRequest, ex);
                throw ex;
            }
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
