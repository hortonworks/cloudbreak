package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol.HTTPS;
import static com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol.TCP_UDP;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsResourceException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ElasticLoadBalancingV2Exception;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;

@ExtendWith(MockitoExtension.class)
class AwsLoadBalancerCommonServiceTest {

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String CIDR = "10.0.0.0/16";

    private static final int PORT = 443;

    private static final String INSTANCE_NAME = "instance";

    private static final String INSTANCE_ID = "instance-id";

    private static final String SUBNET_ID_1 = "subnetId";

    private static final String SUBNET_ID_2 = "anotherSubnetId";

    private static final String PUBLIC_SUBNET_ID_1 = "publicSubnetId";

    private static final String PUBLIC_SUBNET_ID_2 = "anotherPublicSubnetId";

    private static final int HEALTH_CHECK_PORT = 8080;

    private static final String HEALTH_CHECK_PATH = "/";

    private boolean targetGroupStickyness;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @InjectMocks
    private AwsLoadBalancerCommonService underTest;

    @BeforeEach
    public void setup() {
        targetGroupStickyness = false;
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPrivate() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PRIVATE;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PRIVATE_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicEndpointGatway() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicNoEndpointGateway() {
        AwsNetworkView awsNetworkView = createNetworkView(PUBLIC_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPrivateSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class,
                        () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PRIVATE, awsNetworkView,
                                new CloudLoadBalancer(LoadBalancerType.PRIVATE)));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPublicSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class,
                        () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView,
                                new CloudLoadBalancer(LoadBalancerType.PUBLIC)));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsOnlyEndpointGatewaySet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsWhenEndpointGatewaySetAndInstanceGroupNetworkContainsSubnetForMultiAz() {
        AwsNetworkView awsNetworkView = createNetworkView(null, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancerWithEndpointGateay(loadBalancerType, List.of(SUBNET_ID_1, SUBNET_ID_2),
                List.of(PUBLIC_SUBNET_ID_1, PUBLIC_SUBNET_ID_2));

        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);

        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_SUBNET_ID_1, PUBLIC_SUBNET_ID_2), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsWhenSubnetsArePrivateAndInstanceGroupNetworkContainsSubnetForMultiAz() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PRIVATE;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType, List.of(SUBNET_ID_1, SUBNET_ID_2));

        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);

        assertEquals(Set.of(SUBNET_ID_1, SUBNET_ID_2), subnetIds);
    }

    @Test
    public void testConvertLoadBalancerNewPrivate() {
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PRIVATE, PRIVATE_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNAL, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PRIVATE_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerExistingPrivate() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(existingLoadBalancer), LoadBalancerType.PRIVATE, PRIVATE_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(existingLoadBalancer, awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNAL, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PRIVATE_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerNewPublic() {
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PUBLIC, PUBLIC_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNET_FACING, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PUBLIC_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerExistingPublic() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(existingLoadBalancer), LoadBalancerType.PUBLIC, PUBLIC_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(existingLoadBalancer, awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNET_FACING, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PUBLIC_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    void testConvertLoadBalancerCloudLBStikyness() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);
        targetGroupStickyness = true;

        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(LoadBalancerType.PUBLIC);

        AwsLoadBalancer result = underTest.convertLoadBalancer(cloudLoadBalancer, Map.of(INSTANCE_NAME, List.of(INSTANCE_ID)), createNetworkView(PUBLIC_ID_1,
                        null),
                List.of(existingLoadBalancer));

        assertNotNull(result);
        assertFalse(result.getListeners().isEmpty());
        assertNotNull(result.getListeners().get(0).getTargetGroup());
        assertTrue(result.getListeners().get(0).getTargetGroup().isStickySessionEnabled());
    }

    @Test
    void testConvertLoadBalancerCloudLBNoStikyness() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(LoadBalancerType.PUBLIC);

        AwsLoadBalancer result = underTest.convertLoadBalancer(cloudLoadBalancer, Map.of(INSTANCE_NAME, List.of(INSTANCE_ID)), createNetworkView(PUBLIC_ID_1,
                        null),
                List.of(existingLoadBalancer));

        assertNotNull(result);
        assertFalse(result.getListeners().isEmpty());
        assertNotNull(result.getListeners().get(0).getTargetGroup());
        assertFalse(result.getListeners().get(0).getTargetGroup().isStickySessionEnabled());
    }

    @Test
    void testConvertLoadBalancerCloudLBWithoutHealthCheckSettings() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(LoadBalancerType.PUBLIC);

        AwsLoadBalancer result = underTest.convertLoadBalancer(cloudLoadBalancer, Map.of(INSTANCE_NAME, List.of(INSTANCE_ID)), createNetworkView(PUBLIC_ID_1,
                        null),
                List.of(existingLoadBalancer));

        assertNotNull(result);
        assertFalse(result.getListeners().isEmpty());
        AwsListener awsListener = result.getListeners().getFirst();
        AwsTargetGroup targetGroup = awsListener.getTargetGroup();
        assertNotNull(targetGroup);
        assertFalse(targetGroup.isStickySessionEnabled());
        assertNull(awsListener.getProtocol());
        assertEquals(PORT, awsListener.getPort());
        assertEquals(HEALTH_CHECK_PORT, Integer.parseInt(targetGroup.getHealthCheckPort()));
        assertNull(targetGroup.getProtocol());
        assertNull(targetGroup.getHealthCheckPath());
        assertNull(targetGroup.getHealthCheckProtocol());
    }

    @Test
    void testConvertLoadBalancerCloudLBHealthCheckSettings() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancerWithHealthCheckSettings(LoadBalancerType.PUBLIC, List.of());

        AwsLoadBalancer result = underTest.convertLoadBalancer(cloudLoadBalancer, Map.of(INSTANCE_NAME, List.of(INSTANCE_ID)), createNetworkView(PUBLIC_ID_1,
                        null),
                List.of(existingLoadBalancer));

        assertNotNull(result);
        assertFalse(result.getListeners().isEmpty());
        AwsListener awsListener = result.getListeners().getFirst();
        AwsTargetGroup targetGroup = awsListener.getTargetGroup();
        assertNotNull(targetGroup);
        assertFalse(targetGroup.isStickySessionEnabled());
        assertEquals(ProtocolEnum.TCP_UDP, awsListener.getProtocol());
        assertEquals(PORT, awsListener.getPort());
        assertEquals(HEALTH_CHECK_PORT, Integer.parseInt(targetGroup.getHealthCheckPort()));
        assertEquals(ProtocolEnum.TCP_UDP, targetGroup.getProtocol());
        assertEquals(ProtocolEnum.HTTPS, targetGroup.getHealthCheckProtocol());
        assertEquals(HEALTH_CHECK_PATH, targetGroup.getHealthCheckPath());
    }

    @Test
    void testEnableDeletionProtectionWhenLoadBalancerNotFoundExceptionWasThrown() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(LoadBalancerNotFoundException.builder().build());

        assertThrows(AwsResourceException.class, () -> underTest.modifyLoadBalancerAttributes(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testDisableDeletionProtectionWhenLoadBalancerNotFoundExceptionWasThrown() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(LoadBalancerNotFoundException.builder().build());

        assertDoesNotThrow(() -> underTest.disableDeletionProtection(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testDisableDeletionProtectionWhenNoAccessToModifyLoadBalancerAttributes() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(ElasticLoadBalancingV2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
                .build());

        assertDoesNotThrow(() -> underTest.disableDeletionProtection(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testEnableDeletionProtectionWhenNoAccessToModifyLoadBalancerAttributes() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(ElasticLoadBalancingV2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
                .build());

        assertDoesNotThrow(() -> underTest.modifyLoadBalancerAttributes(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testEnableDeletionProtectionWhenAwsServiceExceptionWasThrown() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(AwsServiceException.builder().build());

        assertThrows(AwsServiceException.class, () -> underTest.modifyLoadBalancerAttributes(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testDisableDeletionProtectionWhenAwsServiceExceptionWasThrown() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);
        when(amazonElasticLoadBalancingClient.modifyLoadBalancerAttributes(any())).thenThrow(AwsServiceException.builder().build());

        assertThrows(AwsServiceException.class, () -> underTest.disableDeletionProtection(amazonElasticLoadBalancingClient, "loadBalancerArn"));
    }

    @Test
    void testEnableDeletionProtection() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);

        assertDoesNotThrow(() -> underTest.modifyLoadBalancerAttributes(amazonElasticLoadBalancingClient, "loadBalancerArn"));

        verify(amazonElasticLoadBalancingClient, times(1)).modifyLoadBalancerAttributes(any());
    }

    @Test
    void testDisableDeletionProtection() {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = mock(AmazonElasticLoadBalancingClient.class);

        assertDoesNotThrow(() -> underTest.disableDeletionProtection(amazonElasticLoadBalancingClient, "loadBalancerArn"));

        verify(amazonElasticLoadBalancingClient, times(1)).modifyLoadBalancerAttributes(any());
    }

    @Test
    void testModifyLoadBalancerAttributesSuccess() {
        String arn = "test-arn";
        AmazonElasticLoadBalancingClient client = mock(AmazonElasticLoadBalancingClient.class);

        ArgumentCaptor<ModifyLoadBalancerAttributesRequest> captor = ArgumentCaptor.forClass(ModifyLoadBalancerAttributesRequest.class);

        underTest.modifyLoadBalancerAttributes(client, arn);

        verify(client, times(1)).modifyLoadBalancerAttributes(captor.capture());
        ModifyLoadBalancerAttributesRequest capturedRequest = captor.getValue();

        assertEquals(arn, capturedRequest.loadBalancerArn());
        List<LoadBalancerAttribute> attributes = capturedRequest.attributes();
        assertEquals(2, attributes.size());

        List<String> attrKeys = attributes.stream().map(LoadBalancerAttribute::key).toList();
        assertTrue(attrKeys.contains("deletion_protection.enabled"));
        assertTrue(attrKeys.contains("load_balancing.cross_zone.enabled"));

        attributes.forEach(attribute -> {
            if ("deletion_protection.enabled".equals(attribute.key())) {
                assertEquals("true", attribute.value());
            } else if ("load_balancing.cross_zone.enabled".equals(attribute.key())) {
                assertEquals("true", attribute.value());
            } else {
                fail();
            }
        });
    }

    @Test
    void testLoadBalancerNotFoundExceptionWithDeletionProtectionFalse() {
        String arn = "test-arn";
        AmazonElasticLoadBalancingClient client = mock(AmazonElasticLoadBalancingClient.class);
        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                .errorMessage("No load balancer found with ARN 'test-arn' to enable deletion protection")
                .errorCode("400")
                .build();

        LoadBalancerNotFoundException loadBalancerNotFoundException = LoadBalancerNotFoundException.builder()
                .awsErrorDetails(awsErrorDetails)
                .build();

        doThrow(loadBalancerNotFoundException).when(client).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));

        AwsResourceException exception = assertThrows(AwsResourceException.class,
                () -> underTest.modifyLoadBalancerAttributes(client, arn));

        verify(client, times(1)).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));
        assertTrue(exception.getMessage().contains("No load balancer found with ARN 'test-arn' to enable deletion protection"));
    }

    @Test
    void testAwsServiceExceptionAccessDenied() {
        String arn = "test-arn";
        AmazonElasticLoadBalancingClient client = mock(AmazonElasticLoadBalancingClient.class);

        ElasticLoadBalancingV2Exception exception = (ElasticLoadBalancingV2Exception) ElasticLoadBalancingV2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
                .build();

        doThrow(exception).when(client).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));

        underTest.modifyLoadBalancerAttributes(client, arn);

        verify(client, times(1)).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));
    }

    @Test
    void testAwsServiceExceptionOther() {
        String arn = "test-arn";
        AmazonElasticLoadBalancingClient client = mock(AmazonElasticLoadBalancingClient.class);

        AwsServiceException exception = AwsServiceException.builder().build();

        doThrow(exception).when(client).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));

        AwsServiceException thrownException = assertThrows(AwsServiceException.class, () ->
                underTest.modifyLoadBalancerAttributes(client, arn)
        );

        assertEquals(exception, thrownException);
        verify(client, times(1)).modifyLoadBalancerAttributes(any(ModifyLoadBalancerAttributesRequest.class));
    }

    private AwsLoadBalancer setupAndRunConvertLoadBalancer(List<AwsLoadBalancer> existingLoadBalancers, LoadBalancerType type, String subnetId) {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(subnetId, null);

        Map<String, List<String>> instanceIdsByGroupName = instances.stream()
                .collect(Collectors.groupingBy(CloudResource::getGroup, mapping(CloudResource::getInstanceId, toList())));

        return underTest.convertLoadBalancer(createCloudLoadBalancer(type), instanceIdsByGroupName, awsNetworkView, existingLoadBalancers);
    }

    private List<CloudResource> createInstances() {
        return List.of(CloudResource.builder()
                .withName(INSTANCE_NAME)
                .withInstanceId(INSTANCE_ID)
                .withType(AWS_INSTANCE)
                .withStatus(CREATED)
                .withParameters(Map.of())
                .withGroup(INSTANCE_NAME)
                .build());
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerType type) {
        return createCloudLoadBalancer(type, List.of());
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerType type, List<String> instanceGroupNetworkSubnetIds) {
        Group group = Group.builder()
                .withName(INSTANCE_NAME)
                .withNetwork(createGroupNetwork(instanceGroupNetworkSubnetIds))
                .build();
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type, LoadBalancerSku.getDefault(), targetGroupStickyness);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, HEALTH_CHECK_PORT), Set.of(group));
        return cloudLoadBalancer;
    }

    private CloudLoadBalancer createCloudLoadBalancerWithHealthCheckSettings(LoadBalancerType type, List<String> instanceGroupNetworkSubnetIds) {
        Group group = Group.builder()
                .withName(INSTANCE_NAME)
                .withNetwork(createGroupNetwork(instanceGroupNetworkSubnetIds))
                .build();
        HealthProbeParameters probeParams = new HealthProbeParameters(HEALTH_CHECK_PATH, HEALTH_CHECK_PORT, HTTPS, 10, 2);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type, LoadBalancerSku.getDefault(), targetGroupStickyness);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, TCP_UDP, probeParams), Set.of(group));
        return cloudLoadBalancer;
    }

    private CloudLoadBalancer createCloudLoadBalancerWithEndpointGateay(LoadBalancerType type, List<String> instanceGroupNetworkSubnetIds,
            List<String> endpointGatewaySubnetIds) {
        Group group = Group.builder()
                .withName(INSTANCE_NAME)
                .withNetwork(createGroupNetwork(instanceGroupNetworkSubnetIds, endpointGatewaySubnetIds))
                .build();
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, HEALTH_CHECK_PORT), Set.of(group));
        return cloudLoadBalancer;
    }

    private GroupNetwork createGroupNetwork(List<String> instanceGroupNetworkSubnetIds) {
        Set<GroupSubnet> groupSubnets = instanceGroupNetworkSubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, groupSubnets, new HashMap<>());
    }

    private GroupNetwork createGroupNetwork(List<String> instanceGroupNetworkSubnetIds, List<String> endpointGatewaySubnetIds) {
        Set<GroupSubnet> groupSubnets = instanceGroupNetworkSubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        Set<GroupSubnet> endpointGatewaySubnets = endpointGatewaySubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, groupSubnets, endpointGatewaySubnets, new HashSet<>(), new HashMap<>());
    }

    private AwsNetworkView createNetworkView(String subnetId, String endpointGatewaSubnetId) {
        return new AwsNetworkView(createNetwork(subnetId, endpointGatewaSubnetId));
    }

    private Network createNetwork(String subnetId, String endpointGatewaSubnetId) {
        Map<String, Object> params = new HashMap<>();
        params.put(SUBNET_ID, subnetId);
        params.put(VPC_ID, VPC_ID);
        if (StringUtils.isNotEmpty(endpointGatewaSubnetId)) {
            params.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaSubnetId);
        }
        return new Network(new Subnet(CIDR), params);
    }
}
