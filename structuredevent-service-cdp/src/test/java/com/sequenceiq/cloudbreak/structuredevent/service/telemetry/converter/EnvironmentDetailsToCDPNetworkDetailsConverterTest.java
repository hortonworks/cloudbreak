package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class EnvironmentDetailsToCDPNetworkDetailsConverterTest {

    private EnvironmentDetailsToCDPNetworkDetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @BeforeEach()
    public void setUp() {
        underTest = new EnvironmentDetailsToCDPNetworkDetailsConverter();
    }

    @Test
    public void testConversionNetworkShouldReturnNetworkRelatedFields() {

        CloudSubnet privateSubnet = new CloudSubnet();
        privateSubnet.setType(SubnetType.PRIVATE);

        CloudSubnet dwxSubnet = new CloudSubnet();
        dwxSubnet.setType(SubnetType.DWX);

        CloudSubnet mlxSubnet = new CloudSubnet();
        mlxSubnet.setType(SubnetType.MLX);

        CloudSubnet publicSubnet1 = new CloudSubnet();
        publicSubnet1.setType(SubnetType.PUBLIC);

        CloudSubnet publicSubnet2 = new CloudSubnet();
        publicSubnet2.setType(SubnetType.PUBLIC);

        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of(
                        "1", dwxSubnet,
                        "2", mlxSubnet,
                        "3", publicSubnet1,
                        "4", publicSubnet2,
                        "5", privateSubnet
                        )
                )
                .withEndpointGatewaySubnetMetas(Map.of(
                                "1", publicSubnet1,
                                "2", publicSubnet2,
                                "3", privateSubnet
                        )
                )
                .build();

        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(environmentDetails.getSecurityAccessType()).thenReturn("CIDR_WIDE_OPEN");
        when(environmentDetails.getTunnel()).thenReturn(Tunnel.CCM);
        when(environmentDetails.getDomain()).thenReturn("cldr.com");
        when(environmentDetails.getTlsType()).thenReturn(CcmV2TlsType.ONE_WAY_TLS);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals("EXISTING",
                networkDetails.getNetworkType());
        assertEquals("ENABLED",
                networkDetails.getServiceEndpointCreation());
        assertEquals("EXISTING",
                networkDetails.getNetworkType());
        assertEquals(3,
                networkDetails.getNumberPrivateSubnets());
        assertEquals(2,
                networkDetails.getNumberPublicSubnets());
        assertEquals(1,
                networkDetails.getNumberPrivateLoadBalancerSubnets());
        assertEquals(2,
                networkDetails.getNumberPublicLoadBalancerSubnets());
        assertEquals("DISABLED",
                networkDetails.getPublicEndpointAccessGateway());
        assertEquals("CIDR_WIDE_OPEN",
                networkDetails.getSecurityAccessType());
        assertEquals("CCM",
                networkDetails.getConnectivity());
        assertEquals("ONE_WAY_TLS",
                networkDetails.getControlPlaneAndCCMAgentConnectionSecurity());
        assertEquals("cldr.com",
                networkDetails.getDomain());
    }

    @Test
    public void testConversionPublicEndpointAccessGateway() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
                .build();

        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals("ENABLED",
                networkDetails.getPublicEndpointAccessGateway());
    }

    @Test
    public void testConversionProxyWhenProxyPresentedShouldReturnProxyTrueWithAuth() {
        ProxyDetails proxyDetails = ProxyDetails.Builder.builder()
                .withEnabled(true)
                .withProtocol("http")
                .withAuthentication(true)
                .build();
        when(environmentDetails.getProxyDetails()).thenReturn(proxyDetails);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(true,
                networkDetails.getProxyDetails().getProxy());
        assertEquals("http",
                networkDetails.getProxyDetails().getProtocol());
        assertEquals("BASIC",
                networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testConversionProxyWhenProxyPresentedShouldReturnProxyTrueNoAuth() {
        ProxyDetails proxyDetails = ProxyDetails.Builder.builder()
                .withEnabled(true)
                .withProtocol("https")
                .withAuthentication(false)
                .build();
        when(environmentDetails.getProxyDetails()).thenReturn(proxyDetails);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(true,
                networkDetails.getProxyDetails().getProxy());
        assertEquals("https",
                networkDetails.getProxyDetails().getProtocol());
        assertEquals("NONE",
                networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testConversionProxyWhenProxyNotPresentedShouldReturnProxyFalse() {
        when(environmentDetails.getProxyDetails()).thenReturn(ProxyDetails.Builder.builder().build());

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(false,
                networkDetails.getProxyDetails().getProxy());
        assertEquals("",
                networkDetails.getProxyDetails().getProtocol());
        assertEquals("",
                networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals("", networkDetails.getNetworkType());
        assertEquals("", networkDetails.getServiceEndpointCreation());
        assertEquals("", networkDetails.getConnectivity());
        assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        assertEquals(-1, networkDetails.getNumberPublicSubnets());
        assertEquals("", networkDetails.getPublicEndpointAccessGateway());
        assertEquals("", networkDetails.getSecurityAccessType());
        assertFalse(networkDetails.getProxyDetails().getProxy());
        assertEquals("", networkDetails.getProxyDetails().getProtocol());
        assertEquals("", networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testSettingSubnetNumbersWhenNetworkIsNull() {
        when(environmentDetails.getNetwork()).thenReturn(null);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        assertEquals(-1, networkDetails.getNumberPublicSubnets());
    }

    @Test
    public void testSettingSubnetNumbersWhenSubnetMetasIsNull() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .build();
        networkDto.setSubnetMetas(null);
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        assertEquals(-1, networkDetails.getNumberPublicSubnets());
    }

    @Test
    public void testSettingSubnetNumbersWhenSubnetMetasIsEmpty() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(null)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(0, networkDetails.getNumberPrivateSubnets());
        assertEquals(0, networkDetails.getNumberPublicSubnets());
    }

    @Test
    public void testSettingSubnetNumbersWhenSubnetTypeIsEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        assertEquals(-1, networkDetails.getNumberPublicSubnets());
    }

    @Test
    public void testSettingSubnetNumbersWhenSubnetTypeIsNotEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        publicSubnet.setType(SubnetType.PUBLIC);
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertEquals(0, networkDetails.getNumberPrivateSubnets());
        assertEquals(1, networkDetails.getNumberPublicSubnets());
        assertEquals(0, networkDetails.getNumberFlexibleServerSubnetIds());
    }

    @Test
    public void testSetOwnDnsZoneWhenAzurePrivateDnsZonePresent() {
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(AzureParams.builder()
                        .withDatabasePrivateDnsZoneId("privateDnsZoneId")
                        .withFlexibleServerSubnetIds(Set.of("subnet1", "subnet2"))
                        .build()
                )
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(environmentDetails.getCloudPlatform()).thenReturn("AZURE");

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertTrue(networkDetails.getOwnDnsZones().getPostgres());
        assertEquals(2, networkDetails.getNumberFlexibleServerSubnetIds());
    }

    @Test
    public void testSetOwnDnsZoneWhenAzurePrivateDnsZoneNull() {
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(AzureParams.builder()
                        .withDatabasePrivateDnsZoneId(null)
                        .build()
                )
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(environmentDetails.getCloudPlatform()).thenReturn("AZURE");

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertFalse(networkDetails.getOwnDnsZones().getPostgres());
        assertEquals(0, networkDetails.getNumberFlexibleServerSubnetIds());
    }

    @Test
    public void testSetOwnDnsZoneWhenAzureParamsNull() {
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(null)
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(environmentDetails.getCloudPlatform()).thenReturn("AZURE");

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertFalse(networkDetails.getOwnDnsZones().getPostgres());
        assertEquals(0, networkDetails.getNumberFlexibleServerSubnetIds());
    }

    @Test
    public void testSetOwnDnsZoneWhenNotAzure() {
        when(environmentDetails.getCloudPlatform()).thenReturn("someOtherPlatform");

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertFalse(networkDetails.getOwnDnsZones().getPostgres());
    }

    @Test
    public void testSetOwnDnsZoneWhenCloudPlatformNull() {
        when(environmentDetails.getCloudPlatform()).thenReturn(null);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertFalse(networkDetails.getOwnDnsZones().getPostgres());
    }

    @Test
    public void testSetOwnDnsZoneWhenNetworkDtoNull() {
        when(environmentDetails.getCloudPlatform()).thenReturn("AZURE");

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        assertNotNull(networkDetails.getOwnDnsZones());
        assertFalse(networkDetails.getOwnDnsZones().getPostgres());
    }

}
