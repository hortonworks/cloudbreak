package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil.TO_PORT;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.PrefixListId;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

@ExtendWith(MockitoExtension.class)
class SecurityGroupBuilderUtilTest {

    private static final String REGION_NAME = "regionName";

    @InjectMocks
    private SecurityGroupBuilderUtil underTest;

    @Mock
    private Group group;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private CloudContext context;

    @Mock
    private AwsNetworkView awsNetworkView;

    @Mock
    private Network network;

    @Mock
    private AwsResourceNameService resourceNameService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private AwsContext awsContext;

    @Test
    void testSecurityGroupWhenCloudSecurityIdsNotEmptyThenCreateSecurityGroupAndIngressButNotEgressCreateCalled() {
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(resourceNameService.securityGroup(any(), any(), any())).thenReturn("secGroupName");
        String groupId = "groupId";
        when(amazonEc2Client.createSecurityGroup(any())).thenReturn(CreateSecurityGroupResponse.builder().groupId(groupId).build());
        stubRegionName();
        Security security = mock(Security.class);
        SecurityRule securityRule = new SecurityRule("0.0.0.0/10", new PortDefinition[] { new PortDefinition("22", "22") }, "tcp");
        when(security.getRules()).thenReturn(List.of(securityRule));
        when(group.getSecurity()).thenReturn(security);

        String actual = underTest.createSecurityGroup(network, group, amazonEc2Client, context, ac);

        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(1)).createSecurityGroup(any());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(1)).addIngress(any());
    }

    @Test
    void testCreateOrGetSecurityGroupWhenCreateSecurityGroupCallFails() {
        String groupName = "groupName";
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .groupName(groupName)
                .build();
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("Internal server error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("Internal.Error")
                        .build())
                .build();
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        Assertions.assertThrows(RuntimeException.class,
                () -> underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac));
        verify(amazonEc2Client, times(0)).addIngress(any());
    }

    @Test
    void testCreateOrGetSecurityGroupWhenSecurityGroupExistsAndPermissionsEmpty() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .groupName(groupName)
                .build();
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("Duplicate error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidGroup.Duplicate")
                        .build())
                .build();
        stubRegionName();
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(DescribeSecurityGroupsResponse.builder()
                .securityGroups(SecurityGroup.builder()
                        .groupId(groupId)
                        .groupName(groupName)
                        .build())
                .build());
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        String actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(0)).addIngress(any());
    }

    @Test
    void testCreateOrGetSecurityGroupWhenSecurityGroupExistsAndPermissionsNotEmpty() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .groupName(groupName)
                .build();
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("Duplicate error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidGroup.Duplicate")
                        .build())
                .build();
        stubRegionName();
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(DescribeSecurityGroupsResponse.builder()
                .securityGroups(SecurityGroup.builder()
                        .groupId(groupId)
                        .groupName(groupName)
                        .ipPermissions(IpPermission.builder().build())
                        .build())
                .build());
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        String actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(0)).addIngress(any());
    }

    @Test
    void testCreateOrGetSecurityGroupWhenSecurityGroupExistsVpcIdMismatch() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .groupName(groupName)
                .build();
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("Duplicate error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidGroup.Duplicate")
                        .build())
                .build();
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(DescribeSecurityGroupsResponse.builder()
                .securityGroups(SecurityGroup.builder()
                        .groupId(groupId)
                        .groupName("othergroup")
                        .ipPermissions(IpPermission.builder().build())
                        .build())
                .build());
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        NotFoundException actual = Assertions.assertThrows(NotFoundException.class,
                () -> underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac));
        Assertions.assertEquals("Aws Security Group 'groupName' not found.", actual.getMessage());
    }

    @Test
    void testIngressWhenRulesNotEmpty() {
        String securityGroupId = "secGroupId";

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        Security security = mock(Security.class);
        SecurityRule securityRule = new SecurityRule("0.0.0.0/10", new PortDefinition[] { new PortDefinition("22", "22") }, "tcp");
        when(security.getRules()).thenReturn(List.of(securityRule));
        when(group.getSecurity()).thenReturn(security);

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroupId);

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        Assertions.assertEquals(1, value.ipPermissions().size());
        IpPermission permission = IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(22)
                .toPort(22)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/10").build())
                .build();
        Assertions.assertEquals(permission, value.ipPermissions().get(0));
    }

    @Test
    void testIngressWhenRulesNotEmptyWhenRuleDuplicated() {
        String securityGroupId = "secGroupId";

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        Security security = mock(Security.class);
        SecurityRule securityRule1 = new SecurityRule("0.0.0.0/10", new PortDefinition[] { new PortDefinition("22", "22") }, "tcp");
        SecurityRule securityRule21 = new SecurityRule("0.0.0.0/10", new PortDefinition[] { new PortDefinition("22", "22") }, "tcp");
        when(security.getRules()).thenReturn(List.of(securityRule1, securityRule21));
        when(group.getSecurity()).thenReturn(security);

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroupId);

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        Assertions.assertEquals(1, value.ipPermissions().size());
        IpPermission permission = IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(22)
                .toPort(22)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/10").build())
                .build();
        Assertions.assertEquals(permission, value.ipPermissions().get(0));
    }

    @Test
    void testIngressWhenVpcSubnetNotEmpty() {
        Security security = mock(Security.class);

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        when(security.getRules()).thenReturn(List.of());
        when(group.getSecurity()).thenReturn(security);
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("0.0.0.0/10"));

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, "groupId");

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        //we need to sort, because we need to guaranteed a fix order to check the elements
        List<IpPermission> sorted = value.ipPermissions().stream().sorted(Comparator.comparing(IpPermission::ipProtocol)).collect(Collectors.toList());

        IpPermission permission = IpPermission.builder()
                .ipProtocol("icmp")
                .fromPort(-1)
                .toPort(-1)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/10").build())
                .build();
        Assertions.assertEquals(permission, sorted.get(0));
        IpPermission permission1 = IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(0)
                .toPort(TO_PORT)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/10").build())
                .build();
        Assertions.assertEquals(permission1, sorted.get(1));
        IpPermission permission2 = IpPermission.builder()
                .ipProtocol("udp")
                .fromPort(0)
                .toPort(TO_PORT)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/10").build())
                .build();
        Assertions.assertEquals(permission2, sorted.get(2));
    }

    @Test
    void testEgressWhenOutboundInternetTrafficEnabledButPrefixListAndVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(0)).revokeEgress(any());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficEnabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(List.of("id1"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(0)).revokeEgress(any());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficEnabledAndVpcCidrsNotEmptyButPrefixListEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("id1"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(0)).revokeEgress(any());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledButPrefixListAndVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(0)).revokeEgress(any());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(List.of("id1", "id2"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());
        verify(amazonEc2Client, times(1)).revokeEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().groupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().ipPermissions().get(0).ipProtocol());
        Assertions.assertEquals(0, egressCaptor.getValue().ipPermissions().get(0).fromPort());
        Assertions.assertEquals(TO_PORT, egressCaptor.getValue().ipPermissions().get(0).toPort());
        Assertions.assertEquals("id1", egressCaptor.getValue().ipPermissions().get(0).prefixListIds().get(0).prefixListId());
        Assertions.assertEquals("id2", egressCaptor.getValue().ipPermissions().get(1).prefixListIds().get(0).prefixListId());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledAndVpcCidrsNotEmptyButPrefixListEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());
        verify(amazonEc2Client, times(1)).revokeEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().groupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().ipPermissions().get(0).ipProtocol());
        Assertions.assertEquals("cidr1", egressCaptor.getValue().ipPermissions().get(0).ipRanges().get(0).cidrIp());
        Assertions.assertEquals("cidr2", egressCaptor.getValue().ipPermissions().get(1).ipRanges().get(0).cidrIp());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmptyButContainsAlready() {
        IpPermission ipPermission = IpPermission.builder().ipProtocol("-1")
                .fromPort(0)
                .toPort(TO_PORT)
                .prefixListIds(PrefixListId.builder().prefixListId("id1").build())
                .build();
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(List.of("id1", "id2"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", List.of(ipPermission));
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());
        verify(amazonEc2Client, times(1)).revokeEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().groupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().ipPermissions().get(0).ipProtocol());
        Assertions.assertEquals(0, egressCaptor.getValue().ipPermissions().get(0).fromPort());
        Assertions.assertEquals(TO_PORT, egressCaptor.getValue().ipPermissions().get(0).toPort());
        Assertions.assertEquals("id2", egressCaptor.getValue().ipPermissions().get(0).prefixListIds().get(0).prefixListId());
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledAndVpcCidrsNotEmptyButPrefixListEmptyButContainsAlready() {
        IpPermission cidrPermission = IpPermission.builder().ipProtocol("-1").ipRanges(IpRange.builder().cidrIp("cidr1").build()).build();
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", List.of(cidrPermission));
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());
        verify(amazonEc2Client, times(1)).revokeEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().groupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().ipPermissions().get(0).ipProtocol());
        Assertions.assertEquals("cidr2", egressCaptor.getValue().ipPermissions().get(0).ipRanges().get(0).cidrIp());
    }

    @Test
    void testGetSecurityGroupIdWhenHasMoreThanOneSecurityGroupButNeedToSelectTheSecond() {
        CloudResource secGroupResource1 = getSecurityGroupResourceBuilder().withGroup("groupName1").withReference("ref1").build();
        CloudResource secGroupResource2 = getSecurityGroupResourceBuilder().withGroup("groupName2").withReference("ref2").build();

        when(group.getName()).thenReturn("groupName2");
        when(awsContext.getGroupResources("groupName2")).thenReturn(List.of(secGroupResource1, secGroupResource2));
        List<String> actual = underTest.getSecurityGroupIds(awsContext, group);
        assertThat(actual).containsExactly("ref2");
    }

    @Test
    void testGetSecurityGroupIdsFromGroupReturnsAll() {
        when(group.getName()).thenReturn("groupName2");
        when(group.getSecurity()).thenReturn(security);
        when(security.getCloudSecurityIds()).thenReturn(List.of("sg1", "sg2", "sg3"));

        List<String> actual = underTest.getSecurityGroupIds(awsContext, group);
        assertThat(actual).containsExactly("sg1", "sg2", "sg3");
    }

    @Test
    void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmptyShouldIdempotentWhenEgressRuleExistAndDefaultRevoked() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(List.of("id1", "id2"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());
        Ec2Exception egressAlreadyExists = (Ec2Exception) Ec2Exception.builder()
                .message("Duplicate error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidPermission.Duplicate")
                        .build())
                .build();
        when(amazonEc2Client.addEgress(any())).thenThrow(egressAlreadyExists);
        Ec2Exception defaultEgressRuleAlreadyRevoked = (Ec2Exception) Ec2Exception.builder()
                .message("Not found error")
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("InvalidPermission.NotFound")
                        .build())
                .build();
        when(amazonEc2Client.revokeEgress(any())).thenThrow(defaultEgressRuleAlreadyRevoked);

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());

        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());
        verify(amazonEc2Client, times(1)).revokeEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().groupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().ipPermissions().get(0).ipProtocol());
        Assertions.assertEquals(0, egressCaptor.getValue().ipPermissions().get(0).fromPort());
        Assertions.assertEquals(TO_PORT, egressCaptor.getValue().ipPermissions().get(0).toPort());
        Assertions.assertEquals("id1", egressCaptor.getValue().ipPermissions().get(0).prefixListIds().get(0).prefixListId());
        Assertions.assertEquals("id2", egressCaptor.getValue().ipPermissions().get(1).prefixListIds().get(0).prefixListId());
    }

    private void stubRegionName() {
        when(ac.getCloudContext()).thenReturn(context);
        when(context.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME)));
    }

    private CloudResource.Builder getSecurityGroupResourceBuilder() {
        return CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_SECURITY_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withReference("sg-id")
                .withGroup("groupName")
                .withParameters(emptyMap());
    }
}
