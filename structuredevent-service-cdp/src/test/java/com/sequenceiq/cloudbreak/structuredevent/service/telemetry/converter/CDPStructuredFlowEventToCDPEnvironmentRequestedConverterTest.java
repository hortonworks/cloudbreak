package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.RequestProcessingStepMapper;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
class CDPStructuredFlowEventToCDPEnvironmentRequestedConverterTest {

    private CDPStructuredFlowEventToCDPEnvironmentRequestedConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @Mock
    private EnvironmentFeatures environmentFeatures;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPStructuredFlowEventToCDPEnvironmentRequestedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "requestProcessingStepMapper", new RequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
    }

    @Test
    public void testConversionWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithoutTelemetry() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("", environmentRequested.getTelemetryFeatureDetails().getClusterLogsCollection());
        Assert.assertEquals("", environmentRequested.getTelemetryFeatureDetails().getWorkloadAnalytics());
    }

    @Test
    public void testConversionTelemetry() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        FeatureSetting clusterLogsCollection = new FeatureSetting();
        clusterLogsCollection.setEnabled(Boolean.TRUE);

        FeatureSetting workloadAnalytics = new FeatureSetting();
        workloadAnalytics.setEnabled(Boolean.FALSE);


        when(environmentDetails.getEnvironmentTelemetryFeatures()).thenReturn(environmentFeatures);
        when(environmentFeatures.getClusterLogsCollection()).thenReturn(clusterLogsCollection);
        when(environmentFeatures.getWorkloadAnalytics()).thenReturn(workloadAnalytics);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("true", environmentRequested.getTelemetryFeatureDetails().getClusterLogsCollection());
        Assert.assertEquals("false", environmentRequested.getTelemetryFeatureDetails().getWorkloadAnalytics());
    }

    @Test
    public void testConversionWithoutTunnelShouldUNSETTunnelType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getConnectivity());
    }

    @Test
    public void testConversionTunnelCCMShouldReturnCCMTunnelType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        Tunnel ccmTunnel = Tunnel.CCM;

        when(environmentDetails.getTunnel()).thenReturn(ccmTunnel);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("CCM",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getConnectivity());
    }

    @Test
    public void testConversionTunnelDIRECTShouldReturnDIRECTTunnelType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        Tunnel ccmTunnel = Tunnel.DIRECT;

        when(environmentDetails.getTunnel()).thenReturn(ccmTunnel);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("DIRECT",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getConnectivity());
    }

    @Test
    public void testConversionTunnelCLUSTERPROXYShouldReturnCLUSTERPROXYTunnelType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        Tunnel ccmTunnel = Tunnel.CLUSTER_PROXY;

        when(environmentDetails.getTunnel()).thenReturn(ccmTunnel);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("CLUSTER_PROXY",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getConnectivity());
    }

    @Test
    public void testConversionNetworkShouldReturnNetworkRelatedFields() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        CloudSubnet privateSubnet = new CloudSubnet();
        privateSubnet.setType(SubnetType.PRIVATE);

        CloudSubnet dwxSubnet = new CloudSubnet();
        dwxSubnet.setType(SubnetType.DWX);

        CloudSubnet mlxSubnet = new CloudSubnet();
        mlxSubnet.setType(SubnetType.MLX);

        CloudSubnet publicSubnet = new CloudSubnet();
        publicSubnet.setType(SubnetType.PUBLIC);

        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of(
                        "1", dwxSubnet,
                        "2", mlxSubnet,
                        "3", publicSubnet,
                        "4", privateSubnet
                        )
                )
                .build();

        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("EXISTING",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getNetworkType());
        Assert.assertEquals("ENABLED",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getServiceEndpointCreation());
        Assert.assertEquals("EXISTING",
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getNetworkType());
        Assert.assertEquals(3,
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getNumberPrivateSubnets());
        Assert.assertEquals(1,
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getNumberPublicSubnets());
    }

    @Test
    public void testConversionProxyWhenProxyPresentedShouldReturnProxyTrue() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        when(environmentDetails.getProxyConfigConfigured()).thenReturn(true);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(true,
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getProxyDetails().getProxy());
    }

    @Test
    public void testConversionProxyWhenProxyNotPresentedShouldReturnProxyFalse() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        when(environmentDetails.getProxyConfigConfigured()).thenReturn(false);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(false,
                environmentRequested.getEnvironmentDetails().getNetworkDetails().getProxyDetails().getProxy());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAzureUsingSingleResourceGroupShouldReturnSingleResourceGroupTrue() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(true,
                environmentRequested.getEnvironmentDetails().getAzureDetails().getSingleResourceGroup());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAzureNOTUsingSingleResourceGroupShouldReturnSingleResourceGroupFalse() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(false,
                environmentRequested.getEnvironmentDetails().getAzureDetails().getSingleResourceGroup());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAwsShouldReturnSingleResourceGroupFalse() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        cdpStructuredFlowEvent.setPayload(environmentDetails);

        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentRequested environmentRequested = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(false,
                environmentRequested.getEnvironmentDetails().getAzureDetails().getSingleResourceGroup());
    }
}