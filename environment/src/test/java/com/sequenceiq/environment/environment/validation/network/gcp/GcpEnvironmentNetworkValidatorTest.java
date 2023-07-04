package com.sequenceiq.environment.environment.validation.network.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentNetworkValidatorTest {

    private static final String MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG = "The multiple availability zones feature isn't available for GCP yet. "
            + "Please configure only one zone on field 'availabilityZones'";

    private static final String INVALID_ZONE_PATTERN = "The requested region '%s' doesn't contain the requested '%s' availability zone(s), "
            + "available zones: '%s'";

    private static final String INVALID_REGION_PATTERN = "The environment's requested region '%s' doesn't exist on GCP side.";

    @Mock
    private CloudNetworkService cloudNetworkService;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private ValidationResult.ValidationResultBuilder validationResultBuilder;

    @InjectMocks
    private GcpEnvironmentNetworkValidator underTest;

    @Test
    void testValidateDuringRequestWhenNetworkDtoIsNull() {
        NetworkDto networkDto = null;

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        verifyNoInteractions(validationResultBuilder);
    }

    @Test
    void testValidateDuringRequestWhenGcpParamIsNull() {
        NetworkDto networkDto = NetworkDto.builder().build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        verify(validationResultBuilder, Mockito.times(1)).error(underTest.missingParamsErrorMsg(CloudPlatform.GCP));
    }

    @Test
    void testValidateDuringRequestWhenGcpParamContainsMultipleAvailabilityZones() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1", "gcp-region-zone-2"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        verify(validationResultBuilder, Mockito.times(1)).error(MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG);
    }

    @Test
    void testValidateDuringRequestWhenGcpParamContainsOnlyOneAvailabilityZone() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();

        underTest.validateDuringRequest(networkDto, validationResultBuilder);

        verify(validationResultBuilder, Mockito.times(0)).error(MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG);
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentValidationIsNull() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();

        underTest.validateDuringFlow(null, networkDto, validationResultBuilder);

        verifyNoInteractions(platformParameterService);
        verifyNoInteractions(cloudNetworkService);
        verify(validationResultBuilder).error(anyString());
    }

    @Test
    void testValidateDuringFlowWhenEnvironmentDtoIsNull() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();

        underTest.validateDuringFlow(EnvironmentValidationDto.builder().build(), networkDto, validationResultBuilder);

        verifyNoInteractions(platformParameterService);
        verifyNoInteractions(cloudNetworkService);
        verify(validationResultBuilder).error(anyString());
    }

    @Test
    void testValidateDuringFlowWhenNetworkDtoIsNull() {
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder().build())
                .build();

        underTest.validateDuringFlow(environmentValidationDto, null, validationResultBuilder);

        verifyNoInteractions(platformParameterService);
        verifyNoInteractions(cloudNetworkService);
        verify(validationResultBuilder).error(anyString());
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenGcpParamsDoesNotContainZoneShouldNotAddValidationError() {
        GcpParams gcpParams = GcpParams.builder()
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder().build())
                        .build())
                .build();
        CloudRegions cloudRegions = new CloudRegions(Map.of(), Map.of(), Map.of(), "defaultRegion", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        verifyNoInteractions(validationResultBuilder);
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenGcpParamsContainsEmptyZonesSet() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of())
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder().build())
                        .build())
                .build();

        CloudRegions cloudRegions = new CloudRegions(Map.of(), Map.of(), Map.of(), "defaultRegion", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        verifyNoInteractions(validationResultBuilder);
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenGcpParamsContainsNotEmptyZonesSetButNoCloudRegionsReturned() {
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder().build())
                        .build())
                .build();

        CloudRegions cloudRegions = new CloudRegions(Map.of(), Map.of(), Map.of(), "defaultRegion", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        verifyNoInteractions(validationResultBuilder);
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenGcpParamsContainsNotEmptyZonesSetButRequestedGcpRegionDoesNotExist() {
        Region region = Region.region("region");
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder()
                                .withName(region.getRegionName())
                                .build())
                        .build())
                .build();

        Map<Region, List<AvailabilityZone>> zonesByRegion = Map.of(Region.region("anotherRegion"), List.of(availabilityZone("gcp-region-zone-2")));
        CloudRegions cloudRegions = new CloudRegions(zonesByRegion, Map.of(), Map.of(), "region", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        String expectedErrorMsg = String.format(INVALID_REGION_PATTERN, region.getRegionName());
        verify(validationResultBuilder, times(1)).error(expectedErrorMsg);
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenCloudRegionsZoneSetDoesNotContainTheRequestedZones() {
        String reqInvalidZone = "gcp-region-zone-1";
        String reqValidZone = "gcp-region-zone-2";
        String validZone = "gcp-region-zone-4";
        Region region = Region.region("region");
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of(reqInvalidZone, reqValidZone))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder()
                                .withName(region.getRegionName())
                                .build())
                        .build())
                .build();

        List<AvailabilityZone> providerZones = List.of(availabilityZone(reqValidZone), availabilityZone(validZone));
        Map<Region, List<AvailabilityZone>> zonesByRegion = Map.of(region, providerZones);
        CloudRegions cloudRegions = new CloudRegions(zonesByRegion, Map.of(), Map.of(), "region", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        String expectedErrorMsg = String.format(INVALID_ZONE_PATTERN,
                region.getRegionName(),
                reqInvalidZone,
                providerZones.stream().map(AvailabilityZone::value).collect(Collectors.joining(",")));
        verify(validationResultBuilder, times(1)).error(expectedErrorMsg);
    }

    @Test
    void testCheckAvailabilityZoneDuringValidateDuringFlowWhenAllTheRequestedRegionExistOnGcpSide() {
        Region region = Region.region("region");
        GcpParams gcpParams = GcpParams.builder()
                .withAvailabilityZones(Set.of("gcp-region-zone-1", "gcp-region-zone-2"))
                .build();
        NetworkDto networkDto = NetworkDto.builder()
                .withGcp(gcpParams)
                .build();
        EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                .withEnvironmentDto(EnvironmentDto.builder()
                        .withLocationDto(LocationDto.builder()
                                .withName(region.getRegionName())
                                .build())
                        .build())
                .build();

        List<AvailabilityZone> zonesFromProvider = List.of(availabilityZone("gcp-region-zone-1"), availabilityZone("gcp-region-zone-2"));
        Map<Region, List<AvailabilityZone>> zonesByRegion = Map.of(region, zonesFromProvider);
        CloudRegions cloudRegions = new CloudRegions(zonesByRegion, Map.of(), Map.of(), "region", Boolean.TRUE);
        when(platformParameterService.getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE))).thenReturn(cloudRegions);

        underTest.validateDuringFlow(environmentValidationDto, networkDto, validationResultBuilder);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(any(EnvironmentDto.class), any(NetworkDto.class));
        verifyNoMoreInteractions(cloudNetworkService);
        verify(platformParameterService, times(1)).getRegionsByCredential(any(), Mockito.eq(Boolean.TRUE));
        verifyNoInteractions(validationResultBuilder);
    }
}