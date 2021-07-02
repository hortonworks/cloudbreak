package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
public class EnvironmentDetailsToCDPEnvironmentDetailsConverterTest {

    private EnvironmentDetailsToCDPEnvironmentDetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @BeforeEach()
    public void setUp() {
        underTest = new EnvironmentDetailsToCDPEnvironmentDetailsConverter();
        Whitebox.setInternalState(underTest, "networkDetailsConverter", new EnvironmentDetailsToCDPNetworkDetailsConverter());
    }

    @Test
    public void testNull() {
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(null);

        Assertions.assertEquals("", cdpEnvironmentDetails.getRegion());
        Assertions.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, cdpEnvironmentDetails.getEnvironmentType());
        Assertions.assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
        Assertions.assertNotNull(cdpEnvironmentDetails.getNetworkDetails());
        Assertions.assertNotNull(cdpEnvironmentDetails.getAwsDetails());
        Assertions.assertNotNull(cdpEnvironmentDetails.getAzureDetails());
        Assertions.assertEquals("", cdpEnvironmentDetails.getUserTags());
    }

    @Test
    public void testConvertingEmptyEnvironmentDetails() {
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("", cdpEnvironmentDetails.getRegion());
        Assertions.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, cdpEnvironmentDetails.getEnvironmentType());
        Assertions.assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
        Assertions.assertNotNull(cdpEnvironmentDetails.getNetworkDetails());
        Assertions.assertNotNull(cdpEnvironmentDetails.getAwsDetails());
        Assertions.assertNotNull(cdpEnvironmentDetails.getAzureDetails());
        Assertions.assertEquals("", cdpEnvironmentDetails.getUserTags());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAzureUsingSingleResourceGroupShouldReturnSingleResourceGroupTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertTrue(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAzureNOTUsingSingleResourceGroupShouldReturnSingleResourceGroupFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertFalse(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    public void testConversionResourceEncryptionEnabledWhenAzureUsingResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledTrue() {
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

        Assertions.assertTrue(cdpEnvironmentDetails.getAzureDetails().getResourceEncryptionEnabled());
    }

    @Test
    public void testConversionResourceEncryptionEnabledWhenAzureNOTResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(AzureParametersDto.builder()
                        .withResourceGroup(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertFalse(cdpEnvironmentDetails.getAzureDetails().getResourceEncryptionEnabled());
    }

    @Test
    public void testConversionSingleResourceGroupWhenAwsShouldReturnSingleResourceGroupFalse() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAwsParameters(AwsParametersDto.builder()
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertFalse(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    public void testRegionNameConversion() {
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

        Assertions.assertEquals("invalidregion,uksouth,westus2", cdpEnvironmentDetails.getRegion());
    }

    @Test
    public void testSettingAvailabilityZonesWhenNetworkIsNull() {
        when(environmentDetails.getNetwork()).thenReturn(null);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    public void testSettingAvailabilityZonesWhenSubnetMetasIsNull() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .build();
        networkDto.setSubnetMetas(null);
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    public void testSettingAvailabilityZonesWhenSubnetMetasIsEmpty() {
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(null)
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(0, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    public void testSettingAvailabilityZonesWhenSubnetAvailabilityZoneIsEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(-1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    public void testSettingAvailabilityZonesWhenSubnetAvailabilityZoneIsNotEmpty() {
        CloudSubnet publicSubnet = new CloudSubnet();
        publicSubnet.setAvailabilityZone("availibilityzone");
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withSubnetMetas(Map.of("1", publicSubnet))
                .build();
        when(environmentDetails.getNetwork()).thenReturn(networkDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals(1, cdpEnvironmentDetails.getNumberOfAvailabilityZones());
        Assertions.assertEquals("availibilityzone", cdpEnvironmentDetails.getAvailabilityZones());
    }

    @Test
    public void testUserTags() {
        when(environmentDetails.getUserDefinedTags()).thenReturn(null);
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("", cdpEnvironmentDetails.getUserTags());

        Map<String, String> userTags = new HashMap<>();
        when(environmentDetails.getUserDefinedTags()).thenReturn(userTags);
        cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("", cdpEnvironmentDetails.getUserTags());

        userTags = new HashMap<>();
        userTags.put("key1", "value1");
        userTags.put("key2", "value2");
        when(environmentDetails.getUserDefinedTags()).thenReturn(userTags);
        cdpEnvironmentDetails = underTest.convert(environmentDetails);

        Assertions.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", cdpEnvironmentDetails.getUserTags());
    }
}
