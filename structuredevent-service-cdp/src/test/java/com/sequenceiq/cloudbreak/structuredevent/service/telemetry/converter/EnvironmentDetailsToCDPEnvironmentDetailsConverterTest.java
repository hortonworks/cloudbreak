package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
class EnvironmentDetailsToCDPEnvironmentDetailsConverterTest {

    private EnvironmentDetailsToCDPEnvironmentDetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @BeforeEach()
    void setUp() {
        underTest = new EnvironmentDetailsToCDPEnvironmentDetailsConverter();
        ReflectionTestUtils.setField(underTest, "networkDetailsConverter", new EnvironmentDetailsToCDPNetworkDetailsConverter());
    }

    @Test
    void testNull() {
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(null);

        assertEquals("", cdpEnvironmentDetails.getRegion());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, cdpEnvironmentDetails.getEnvironmentType());
        assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
        assertNotNull(cdpEnvironmentDetails.getNetworkDetails());
        assertNotNull(cdpEnvironmentDetails.getAwsDetails());
        assertNotNull(cdpEnvironmentDetails.getAzureDetails());
        assertEquals("", cdpEnvironmentDetails.getUserTags());
    }

    @Test
    void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals("", cdpEnvironmentDetails.getRegion());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, cdpEnvironmentDetails.getEnvironmentType());
        assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
        assertNotNull(cdpEnvironmentDetails.getNetworkDetails());
        assertNotNull(cdpEnvironmentDetails.getAwsDetails());
        assertNotNull(cdpEnvironmentDetails.getAzureDetails());
        assertEquals("", cdpEnvironmentDetails.getUserTags());
    }

    @Test
    void testConversionSingleResourceGroupWhenAzureUsingSingleResourceGroupShouldReturnSingleResourceGroupTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertTrue(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    void testConversionSingleResourceGroupWhenAzureNOTUsingSingleResourceGroupShouldReturnSingleResourceGroupFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenAzureUsingResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl("dummyEncryptionKeyUrl")
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertTrue(cdpEnvironmentDetails.getAzureDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenAzureNOTResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getAzureDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenAWSUsingResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                                .withEncryptionKeyArn("dummyEncryptionKeyArn")
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertTrue(cdpEnvironmentDetails.getAwsDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenAWSNOTResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledFalse() {
        ParametersDto parametersDto = ParametersDto.builder().build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getAwsDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenGcpUsingResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withGcpParameters(GcpParametersDto.builder()
                        .withEncryptionParameters(GcpResourceEncryptionParametersDto.builder()
                                .withEncryptionKey("dummyEncryptionKeyUrl")
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertTrue(cdpEnvironmentDetails.getGcpDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenGcpNOTResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledFalse() {
        ParametersDto parametersDto = ParametersDto.builder().build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getGcpDetails().getResourceEncryptionEnabled());
    }

    @Test
    void testConversionSingleResourceGroupWhenAwsShouldReturnSingleResourceGroupFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    void testRegionNameConversion() {
        Region region1 = new Region();
        region1.setName("westus2");
        Region region2 = new Region();
        region2.setName("UK South");
        Region region3 = new Region();
        region3.setName("Invalid Region");

        Set<Region> regions = new HashSet<>();
        regions.add(region1);
        regions.add(region2);
        regions.add(region3);

        when(environmentDetails.getRegions()).thenReturn(regions);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals("invalidregion,uksouth,westus2", cdpEnvironmentDetails.getRegion());
    }

    @Test
    void testSettingAvailabilityZonesWhenNetworkIsNull() {
        when(environmentDetails.getNetwork()).thenReturn(null);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    void testSettingAvailabilityZonesWhenSubnetMetasIsNull() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .build();
        networkDto.setSubnetMetas(null);
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    void testSettingAvailabilityZonesWhenSubnetMetasIsEmpty() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(null)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(0, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    void testSettingAvailabilityZonesWhenSubnetAvailabilityZoneIsEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    void testSettingAvailabilityZonesWhenSubnetAvailabilityZoneIsNotEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        publicSubnet.setAvailabilityZone("availibilityzone");
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        assertEquals("availibilityzone", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    void testUserTags() {
        when(environmentDetails.getUserDefinedTags()).thenReturn(null);
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals("", cdpEnvironmentDetails.getUserTags());

        Map<String, String> userTags = new HashMap<>();
        when(environmentDetails.getUserDefinedTags()).thenReturn(userTags);
        cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals("", cdpEnvironmentDetails.getUserTags());

        userTags = new HashMap<>();
        userTags.put("key1", "value1");
        userTags.put("key2", "value2");
        when(environmentDetails.getUserDefinedTags()).thenReturn(userTags);
        cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", cdpEnvironmentDetails.getUserTags());
    }
}
