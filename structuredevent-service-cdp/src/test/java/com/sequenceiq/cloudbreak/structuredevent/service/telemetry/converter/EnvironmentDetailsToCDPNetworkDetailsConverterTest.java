package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
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
                .build();

        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(environmentDetails.getSecurityAccessType()).thenReturn("CIDR_WIDE_OPEN");
        when(environmentDetails.getTunnel()).thenReturn(Tunnel.CCM);
        when(environmentDetails.getDomain()).thenReturn("cldr.com");
        when(environmentDetails.getTlsType()).thenReturn(CcmV2TlsType.ONE_WAY_TLS);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assert.assertEquals("EXISTING",
                networkDetails.getNetworkType());
        Assert.assertEquals("ENABLED",
                networkDetails.getServiceEndpointCreation());
        Assert.assertEquals("EXISTING",
                networkDetails.getNetworkType());
        Assert.assertEquals(3,
                networkDetails.getNumberPrivateSubnets());
        Assert.assertEquals(2,
                networkDetails.getNumberPublicSubnets());
        Assert.assertEquals("DISABLED",
                networkDetails.getPublicEndpointAccessGateway());
        Assert.assertEquals("CIDR_WIDE_OPEN",
                networkDetails.getSecurityAccessType());
        Assert.assertEquals("CCM",
                networkDetails.getConnectivity());
        Assert.assertEquals("ONE_WAY_TLS",
                networkDetails.getControlPlaneAndCCMAgentConnectionSecurity());
        Assert.assertEquals("cldr.com",
                networkDetails.getDomain());
    }

    @Test
    public void testConversionPublicEndpointAccessGateway() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withUsePublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
                .build();

        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assert.assertEquals("ENABLED",
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

        Assert.assertEquals(true,
                networkDetails.getProxyDetails().getProxy());
        Assert.assertEquals("http",
                networkDetails.getProxyDetails().getProtocol());
        Assert.assertEquals("BASIC",
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

        Assert.assertEquals(true,
                networkDetails.getProxyDetails().getProxy());
        Assert.assertEquals("https",
                networkDetails.getProxyDetails().getProtocol());
        Assert.assertEquals("NONE",
                networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testConversionProxyWhenProxyNotPresentedShouldReturnProxyFalse() {
        when(environmentDetails.getProxyDetails()).thenReturn(ProxyDetails.Builder.builder().build());

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assert.assertEquals(false,
                networkDetails.getProxyDetails().getProxy());
        Assert.assertEquals("",
                networkDetails.getProxyDetails().getProtocol());
        Assert.assertEquals("",
                networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("", networkDetails.getNetworkType());
        Assertions.assertEquals("", networkDetails.getServiceEndpointCreation());
        Assertions.assertEquals("", networkDetails.getConnectivity());
        Assertions.assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(-1, networkDetails.getNumberPublicSubnets());
        Assertions.assertEquals("", networkDetails.getPublicEndpointAccessGateway());
        Assertions.assertEquals("", networkDetails.getSecurityAccessType());
        Assertions.assertFalse(networkDetails.getProxyDetails().getProxy());
        Assertions.assertEquals("", networkDetails.getProxyDetails().getProtocol());
        Assertions.assertEquals("", networkDetails.getProxyDetails().getAuthentication());
    }

    @Test
    public void testSettingSubnetNumbersWhenNetworkIsNull() {
        when(environmentDetails.getNetwork()).thenReturn(null);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(-1, networkDetails.getNumberPublicSubnets());
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

        Assertions.assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(-1, networkDetails.getNumberPublicSubnets());
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

        Assertions.assertEquals(0, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(0, networkDetails.getNumberPublicSubnets());
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

        Assertions.assertEquals(-1, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(-1, networkDetails.getNumberPublicSubnets());
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

        Assertions.assertEquals(0, networkDetails.getNumberPrivateSubnets());
        Assertions.assertEquals(1, networkDetails.getNumberPublicSubnets());
    }
}