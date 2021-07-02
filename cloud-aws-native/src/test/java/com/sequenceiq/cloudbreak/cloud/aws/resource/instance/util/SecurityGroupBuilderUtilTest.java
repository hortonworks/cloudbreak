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
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeModel;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@ExtendWith(MockitoExtension.class)
public class SecurityGroupBuilderUtilTest {

    @InjectMocks
    private SecurityGroupBuilderUtil underTest;

    @Mock
    private AwsCloudStackView awsCloudStackView;

    @Mock
    private Group group;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private CloudContext context;

    @Mock
    private AwsNativeModel awsNativeModel;

    @Mock
    private AwsNetworkView awsNetworkView;

    @Mock
    private AwsResourceNameService resourceNameService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @BeforeEach
    public void beforeEach() {

    }

    @Test
    public void testSecurityGroupWhenCloudSecurityIdsNotEmptyThenCreateSecurityGroupAndIngressButNotEgressCreateCalled() {
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(resourceNameService.resourceName(any(), any())).thenReturn("secGroupName");
        when(amazonEc2Client.createSecurityGroup(any())).thenReturn(new CreateSecurityGroupResult().withGroupId("groupId"));
        when(awsCloudStackView.network()).thenReturn(awsNetworkView);
        Map<String, String> actual = underTest.createSecurityGroup(awsCloudStackView, group, amazonEc2Client, context, awsNativeModel);

        Assertions.assertFalse(actual.isEmpty());
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
        when(group.getSecurity()).thenReturn(mock(Security.class));
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(new DescribeSecurityGroupsResult()
                .withSecurityGroups(new SecurityGroup().withGroupId(groupId).withGroupName(groupName)));
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        Map<String, String> actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNativeModel);
        Assertions.assertTrue(actual.containsKey("SECURITY_GROUP_ID"));
        Assertions.assertTrue(actual.containsKey("SECURITY_GROUP_NAME"));
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
        when(amazonEc2Client.describeSecurityGroups(any())).thenReturn(new DescribeSecurityGroupsResult()
                .withSecurityGroups(new SecurityGroup()
                        .withGroupId(groupId)
                        .withGroupName(groupName)
                        .withIpPermissions(new IpPermission())));
        when(amazonEc2Client.createSecurityGroup(request)).thenThrow(amazonEC2Exception);

        Map<String, String> actual = underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNativeModel);
        Assertions.assertTrue(actual.containsKey("SECURITY_GROUP_ID"));
        Assertions.assertTrue(actual.containsKey("SECURITY_GROUP_NAME"));
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
                () -> underTest.createOrGetSecurityGroup(amazonEc2Client, request, group, awsNativeModel));
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

        underTest.ingress(group, amazonEc2Client, awsNativeModel, securityGroupId);

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

        underTest.ingress(group, amazonEc2Client, awsNativeModel, securityGroupId);

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
        when(awsNativeModel.getVpcSubnet()).thenReturn(List.of("0.0.0.0/10"));

        underTest.ingress(group, amazonEc2Client, awsNativeModel, "groupId");

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
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(emptyList());
        when(awsNativeModel.getVpcCidrs()).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficEnabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(List.of("id1"));
        when(awsNativeModel.getVpcCidrs()).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficEnabledAndVpcCidrsNotEmptyButPrefixListEmpty() {
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.ENABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(emptyList());
        when(awsNativeModel.getVpcCidrs()).thenReturn(List.of("id1"));

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledButPrefixListAndVpcCidrsEmpty() {
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(emptyList());
        when(awsNativeModel.getVpcCidrs()).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
        verify(amazonEc2Client, times(0)).addEgress(any());
    }

    @Test
    public void testEgressWhenOutboundInternetTrafficDisabledAndPrefixListNotEmptyButVpcCidrsEmpty() {
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(List.of("id1", "id2"));
        when(awsNativeModel.getVpcCidrs()).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
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
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(emptyList());
        when(awsNativeModel.getVpcCidrs()).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, awsNativeModel, "id", emptyList());
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
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(List.of("id1", "id2"));
        when(awsNativeModel.getVpcCidrs()).thenReturn(emptyList());

        underTest.egress(amazonEc2Client, awsNativeModel, "id", List.of(ipPermission));
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
        when(awsNativeModel.getOutboundInternetTraffic()).thenReturn(OutboundInternetTraffic.DISABLED);
        when(awsNativeModel.getPrefixListIds()).thenReturn(emptyList());
        when(awsNativeModel.getVpcCidrs()).thenReturn(List.of("cidr1", "cidr2"));

        underTest.egress(amazonEc2Client, awsNativeModel, "id", List.of(cidrPermission));
        ArgumentCaptor<AuthorizeSecurityGroupEgressRequest> egressCaptor = ArgumentCaptor.forClass(AuthorizeSecurityGroupEgressRequest.class);
        verify(amazonEc2Client).addEgress(egressCaptor.capture());
        verify(amazonEc2Client, times(1)).addEgress(any());

        Assertions.assertEquals("id", egressCaptor.getValue().getGroupId());
        Assertions.assertEquals("-1", egressCaptor.getValue().getIpPermissions().get(0).getIpProtocol());
        Assertions.assertEquals("cidr2", egressCaptor.getValue().getIpPermissions().get(0).getIpv4Ranges().get(0).getCidrIp());
    }

}
