package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
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

    @Mock
    private NetworkDto networkDto;

    private static Stream<Arguments> credentialTypes() {
        return Stream.of(
                Arguments.of(CredentialType.AWS_KEY_BASED, UsageProto.CDPCredentialType.Value.AWS_KEY_BASED),
                Arguments.of(CredentialType.AWS_ROLE_BASED, UsageProto.CDPCredentialType.Value.AWS_ROLE_BASED),
                Arguments.of(CredentialType.AZURE_CODEGRANTFLOW, UsageProto.CDPCredentialType.Value.AZURE_CODEGRANTFLOW),
                Arguments.of(CredentialType.AZURE_APPBASED_SECRET, UsageProto.CDPCredentialType.Value.AZURE_APPBASED_SECRET),
                Arguments.of(CredentialType.AZURE_APPBASED_CERTIFICATE, UsageProto.CDPCredentialType.Value.AZURE_APPBASED_CERTIFICATE),
                Arguments.of(CredentialType.GCP_JSON, UsageProto.CDPCredentialType.Value.GCP_JSON),
                Arguments.of(CredentialType.GCP_P12, UsageProto.CDPCredentialType.Value.GCP_P12),
                Arguments.of(CredentialType.YARN, UsageProto.CDPCredentialType.Value.YARN),
                Arguments.of(CredentialType.MOCK, UsageProto.CDPCredentialType.Value.MOCK),
                Arguments.of(CredentialType.UNKNOWN, UsageProto.CDPCredentialType.Value.UNKNOWN),
                Arguments.of(null, UsageProto.CDPCredentialType.Value.UNKNOWN)
        );
    }

    @BeforeEach()
    void setUp() {
        underTest = new EnvironmentDetailsToCDPEnvironmentDetailsConverter();
        ReflectionTestUtils.setField(underTest, "networkDetailsConverter", new EnvironmentDetailsToCDPNetworkDetailsConverter());
        lenient().when(environmentDetails.creatorClient()).thenReturn("No Info");
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
        assertThat(cdpEnvironmentDetails.getSecretEncryptionEnabled()).isFalse();
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
        assertEquals(UsageProto.CDPCredentialType.Value.UNKNOWN, cdpEnvironmentDetails.getCredentialDetails().getCredentialType());
        assertThat(cdpEnvironmentDetails.getSecretEncryptionEnabled()).isFalse();
    }

    @Test
    void testConversionSingleResourceGroupWhenAzureUsingSingleResourceGroupShouldReturnSingleResourceGroupTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
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
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertFalse(cdpEnvironmentDetails.getAzureDetails().getSingleResourceGroup());
    }

    @Test
    void testConversionAzureWithNetwork() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_MULTIPLE)
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);
        when(environmentDetails.getNetwork()).thenReturn(networkDto);
        when(networkDto.getRegistrationType()).thenReturn(RegistrationType.CREATE_NEW);
        when(networkDto.getServiceEndpointCreation()).thenReturn(ServiceEndpointCreation.DISABLED);
        when(environmentDetails.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.toString());
        Map<String, CloudSubnet> subnets = new HashMap<>();
        CloudSubnet cloudSubnet = new CloudSubnet();
        subnets.put("subnet1", cloudSubnet);
        when(networkDto.getSubnetMetas()).thenReturn(subnets);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(1, cdpEnvironmentDetails.getNetworkDetails().getNumberPrivateSubnets());
    }

    @Test
    void testConversionResourceEncryptionEnabledWhenAzureUsingResourceEncryptionEnabledShouldReturnResourceEncryptionEnabledTrue() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
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
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
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
                .withAwsParametersDto(AwsParametersDto.builder()
                        .withAwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.builder()
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
                .withGcpParametersDto(GcpParametersDto.builder()
                        .withGcpResourceEncryptionParametersDto(GcpResourceEncryptionParametersDto.builder()
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
                .withAwsParametersDto(AwsParametersDto.builder()
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

    @ParameterizedTest
    @MethodSource("credentialTypes")
    void testCredentialDetails(CredentialType credentialType, UsageProto.CDPCredentialType.Value cdpCredentialType) {
        CredentialDetails credentialDetails = CredentialDetails.builder()
                .withCredentialType(credentialType)
                .build();
        when(environmentDetails.getCredentialDetails()).thenReturn(credentialDetails);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(cdpCredentialType, cdpEnvironmentDetails.getCredentialDetails().getCredentialType());
    }

    @Test
    void testCredentialDetailsWhenNull() {
        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(UsageProto.CDPCredentialType.Value.UNKNOWN, cdpEnvironmentDetails.getCredentialDetails().getCredentialType());
    }

    @Test
    void testConversionSecretEncryptionEnabled() {
        when(environmentDetails.isEnableSecretEncryption()).thenReturn(true);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertThat(cdpEnvironmentDetails.getSecretEncryptionEnabled()).isTrue();
    }

    @Test
    void testComputeClusterDetailsConversion() {
        when(environmentDetails.getExternalizedComputeCluster()).thenReturn(ExternalizedComputeClusterDto.builder()
                .withCreate(true)
                .withPrivateCluster(true)
                .withKubeApiAuthorizedIpRanges(Set.of("0.0.0.0/0", "1.1.1.1/1"))
                .withWorkerNodeSubnetIds(Set.of("subnet1", "subnet2"))
                .withOutboundType("udr")
                .build());
        when(environmentDetails.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        UsageProto.CDPComputeClusterDetails computeClusterDetails = cdpEnvironmentDetails.getComputeClusterDetails();
        assertThat(computeClusterDetails).isNotNull();
        assertTrue(computeClusterDetails.getEnabled());
        assertTrue(computeClusterDetails.getPrivateCluster());
        assertThat(computeClusterDetails.getKubeApiAuthorizedIpRangesList()).containsOnly("0.0.0.0/0", "1.1.1.1/1");
        assertThat(computeClusterDetails.getWorkerNodeSubnetIdsList()).containsOnly("subnet1", "subnet2");
        UsageProto.CDPAzureComputeClusterDetails azureComputeClusterDetails = computeClusterDetails.getAzureComputeClusterDetails();
        assertEquals("udr", azureComputeClusterDetails.getOutboundType());
    }

    @Test
    void testEncryptionManagedIdentityConversion() {
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParametersDto(AzureParametersDto.builder()
                        .withAzureResourceGroupDto(AzureResourceGroupDto.builder()
                                .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                .build())
                        .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
                                .withEncryptionKeyUrl("dummyEncryptionKeyUrl")
                                .withUserManagedIdentity("userManagedIdentity")
                                .build())
                        .build())
                .build();

        when(environmentDetails.getParameters()).thenReturn(parametersDto);

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertTrue(cdpEnvironmentDetails.getAzureDetails().getResourceEncryptionEnabled());
        assertEquals("userManagedIdentity", cdpEnvironmentDetails.getAzureDetails().getEncryptionManagedIdentity());
    }

    @Test
    void testEnvironmentDeletionTypeConversion() {
        when(environmentDetails.getEnvironmentDeletionTypeAsString()).thenReturn("FORCE");

        UsageProto.CDPEnvironmentDetails cdpEnvironmentDetails = underTest.convert(environmentDetails);

        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.FORCE, cdpEnvironmentDetails.getEnvironmentDeletionType());
    }
}
