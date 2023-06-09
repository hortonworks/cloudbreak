package com.sequenceiq.environment.environment.validation.network.gcp;

import static com.sequenceiq.environment.environment.validation.network.gcp.GcpEnvironmentNetworkValidator.MULTIPLE_AVAILABILITY_ZONES_PROVIDED_ERROR_MSG;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentNetworkValidatorTest {

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
}