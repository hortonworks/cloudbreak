package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
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
    public void testConversionProxyWhenProxyPresentedShouldReturnProxyTrue() {
        when(environmentDetails.getProxyConfigConfigured()).thenReturn(true);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assert.assertEquals(true,
                networkDetails.getProxyDetails().getProxy());
    }

    @Test
    public void testConversionProxyWhenProxyNotPresentedShouldReturnProxyFalse() {
        when(environmentDetails.getProxyConfigConfigured()).thenReturn(false);

        UsageProto.CDPNetworkDetails networkDetails = underTest.convert(environmentDetails);

        Assert.assertEquals(false,
                networkDetails.getProxyDetails().getProxy());
    }
}