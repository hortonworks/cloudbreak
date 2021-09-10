package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil.TO_PORT;
import static java.util.Collections.emptyList;
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

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.PrefixListId;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@ExtendWith(MockitoExtension.class)
public class SecurityGroupBuilderUtilTest {

    private static final String REGION_NAME = "regionName";

    @InjectMocks
    private SecurityGroupBuilderUtil underTest;

    @Mock
    private Group group;

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

    @Test
    public void testSecurityGroupWhenCloudSecurityIdsNotEmptyThenCreateSecurityGroupAndIngressButNotEgressCreateCalled() {
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(resourceNameService.resourceName(any(), any())).thenReturn("secGroupName");
        String groupId = "groupId";
        when(amazonEc2Client.createSecurityGroup(any())).thenReturn(new CreateSecurityGroupResult().withGroupId(groupId));
        stubRegionName();
        String actual = underTest.createSecurityGroup(network, group, amazonEc2Client, context, ac);

        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(1)).createSecurityGroup(any());
        verify(amazonEc2Client, times(0)).addEgress(any());
        verify(amazonEc2Client, times(1)).addIngress(any());
    }

    @Test
    public void testCreateOrGetSecurityGroupWhenSecurityGroupExistsAndPermissionsEmpty() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setGroupName(groupName);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("Duplicate error");
        amazonEC2Exception.setErrorCode("InvalidGroup.Duplicate");
        stubRegionName();
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(new DescribeSecurityGroupsResult()
                .withSecurityGroups(new SecurityGroup().withGroupId(groupId).withGroupName(groupName)));
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        String actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(1)).addIngress(any());
    }

    @Test
    public void testCreateOrGetSecurityGroupWhenSecurityGroupExistsAndPermissionsNotEmpty() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setGroupName(groupName);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("Duplicate error");
        amazonEC2Exception.setErrorCode("InvalidGroup.Duplicate");
        stubRegionName();
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(new DescribeSecurityGroupsResult()
                .withSecurityGroups(new SecurityGroup()
                        .withGroupId(groupId)
                        .withGroupName(groupName)
                        .withIpPermissions(new IpPermission())));
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        String actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac);
        Assertions.assertEquals(groupId, actual);
        verify(amazonEc2Client, times(0)).addIngress(any());
    }

    @Test
    public void testCreateOrGetSecurityGroupWhenSecurityGroupExistsVpcIdMismatch() {
        String groupId = "groupId";
        String groupName = "groupName";
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setGroupName(groupName);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("Duplicate error");
        amazonEC2Exception.setErrorCode("InvalidGroup.Duplicate");
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(new DescribeSecurityGroupsResult()
                .withSecurityGroups(new SecurityGroup()
                        .withGroupId(groupId)
                        .withGroupName("othergroup")
                        .withIpPermissions(new IpPermission())));
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        NotFoundException actual = Assertions.assertThrows(NotFoundException.class,
                () -> underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNetworkView, ac));
        Assertions.assertEquals("Aws Security Group 'groupName' not found.", actual.getMessage());
    }

    @Test
    public void testIngressWhenRulesNotEmpty() {
        String securityGroupId = "secGroupId";

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        Security security = mock(Security.class);
        SecurityRule securityRule = new SecurityRule("0.0.0.0/10", new PortDefinition[]{new PortDefinition("22", "22")}, "tcp");
        when(security.getRules()).thenReturn(List.of(securityRule));
        when(group.getSecurity()).thenReturn(security);

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroupId);

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        Assertions.assertEquals(1, value.getIpPermissions().size());
        IpPermission permission = new IpPermission()
                .withIpProtocol("tcp")
                .withFromPort(22)
                .withToPort(22)
                .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/10"));
        Assertions.assertEquals(permission, value.getIpPermissions().get(0));
    }

    @Test
    public void testIngressWhenRulesNotEmptyWhenRuleDuplicated() {
        String securityGroupId = "secGroupId";

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        Security security = mock(Security.class);
        SecurityRule securityRule1 = new SecurityRule("0.0.0.0/10", new PortDefinition[]{new PortDefinition("22", "22")}, "tcp");
        SecurityRule securityRule21 = new SecurityRule("0.0.0.0/10", new PortDefinition[]{new PortDefinition("22", "22")}, "tcp");
        when(security.getRules()).thenReturn(List.of(securityRule1, securityRule21));
        when(group.getSecurity()).thenReturn(security);

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, securityGroupId);

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        Assertions.assertEquals(1, value.getIpPermissions().size());
        IpPermission permission = new IpPermission()
                .withIpProtocol("tcp")
                .withFromPort(22)
                .withToPort(22)
                .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/10"));
        Assertions.assertEquals(permission, value.getIpPermissions().get(0));
    }

    @Test
    public void testIngressWhenVpcSubnetNotEmpty() {
        Security security = mock(Security.class);

        ArgumentCaptor<AuthorizeSecurityGroupIngressRequest> ingressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupIngressRequest.class);

        when(security.getRules()).thenReturn(List.of());
        when(group.getSecurity()).thenReturn(security);
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("0.0.0.0/10"));

        underTest.ingress(group, ac, amazonEc2Client, awsNetworkView, "groupId");

        verify(amazonEc2Client).addIngress(ingressCaptor.capture());

        AuthorizeSecurityGroupIngressRequest value = ingressCaptor.getValue();
        //we need to sort, because we need to guaranteed a fix order to check the elements
        List<IpPermission> sorted = value.getIpPermissions().stream().sorted(Comparator.comparing(IpPermission::getIpProtocol)).collect(Collectors.toList());

        IpPermission permission = new IpPermission()
                .withIpProtocol("icmp")
                .withFromPort(-1)
                .withToPort(-1)
                .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/10"));
        Assertions.assertEquals(permission, sorted.get(0));
        IpPermission permission1 = new IpPermission()
                .withIpProtocol("tcp")
                .withFromPort(0)
                .withToPort(TO_PORT)
                .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/10"));
        Assertions.assertEquals(permission1, sorted.get(1));
        IpPermission permission2 = new IpPermission()
                .withIpProtocol("udp")
                .withFromPort(0)
                .withToPort(TO_PORT)
                .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/10"));
        Assertions.assertEquals(permission2, sorted.get(2));
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficEnabledButPrefixListAndVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficEnabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(List.of("id1"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficEnabledAndVpcCidrsNotEmptyButPrefixListEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.ENABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("id1"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledButPrefixListAndVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(List.of("id1", "id2"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().getGroupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().getIpPermissions().get(0).getIpProtocol());
        Assertions.assertEquals(0, egressCaptor.getValue().getIpPermissions().get(0).getFromPort());
        Assertions.assertEquals(TO_PORT, egressCaptor.getValue().getIpPermissions().get(0).getToPort());
        Assertions.assertEquals("id1", egressCaptor.getValue().getIpPermissions().get(0).getPrefixListIds().get(0).getPrefixListId());
        Assertions.assertEquals("id2", egressCaptor.getValue().getIpPermissions().get(1).getPrefixListIds().get(0).getPrefixListId());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledAndVpcCidrsNotEmptyButPrefixListEmpty() {
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", emptyList());
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().getGroupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().getIpPermissions().get(0).getIpProtocol());
        Assertions.assertEquals("cidr1", egressCaptor.getValue().getIpPermissions().get(0).getIpv4Ranges().get(0).getCidrIp());
        Assertions.assertEquals("cidr2", egressCaptor.getValue().getIpPermissions().get(1).getIpv4Ranges().get(0).getCidrIp());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmptyButContainsAlready() {
        IpPermission ipPermission = new IpPermission().withIpProtocol("-1")
                .withFromPort(0)
                .withToPort(TO_PORT)
                .withPrefixListIds(new PrefixListId().withPrefixListId("id1"));
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(List.of("id1", "id2"));
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", List.of(ipPermission));
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().getGroupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().getIpPermissions().get(0).getIpProtocol());
        Assertions.assertEquals(0, egressCaptor.getValue().getIpPermissions().get(0).getFromPort());
        Assertions.assertEquals(TO_PORT, egressCaptor.getValue().getIpPermissions().get(0).getToPort());
        Assertions.assertEquals("id2", egressCaptor.getValue().getIpPermissions().get(0).getPrefixListIds().get(0).getPrefixListId());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledAndVpcCidrsNotEmptyButPrefixListEmptyButContainsAlready() {
        IpPermission cidrPermission = new IpPermission().withIpProtocol("-1").withIpv4Ranges(new IpRange().withCidrIp("cidr1"));
        stubRegionName();
        when(awsNetworkView.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNetworkService.getPrefixListIds(amazonEc2Client, REGION_NAME, OutboundInternetTraffic.DISABLED)).thenReturn(emptyList());
        when(awsNetworkService.getVpcCidrs(ac, awsNetworkView)).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, ac, awsNetworkView, "id", List.of(cidrPermission));
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().getGroupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().getIpPermissions().get(0).getIpProtocol());
        Assertions.assertEquals("cidr2", egressCaptor.getValue().getIpPermissions().get(0).getIpv4Ranges().get(0).getCidrIp());
    }

    private void stubRegionName() {
        when(ac.getCloudContext()).thenReturn(context);
        when(context.getLocation()).thenReturn(Location.location(Region.region(REGION_NAME)));
    }
}
