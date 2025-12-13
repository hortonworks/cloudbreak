package com.sequenceiq.environment.platformresource.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.platformresource.v1.converter.CloudVmTypesToPlatformVmTypesV1ResponseConverter;

@ExtendWith(MockitoExtension.class)
public class EnvironmentPlatformResourceControllerTest {
    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:de7c7a21-8f5a-4c3a-bd5c-2d17f57d1879";

    private static final String CURRENT_USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Mock
    private CloudVmTypesToPlatformVmTypesV1ResponseConverter cloudVmTypesToPlatformVmTypesV1ResponseConverter;

    @InjectMocks
    private EnvironmentPlatformResourceController underTest;

    @Test
    void testGetVmTypesForVerticalScalingWithAvailabilityZonesFilterAsNull() {
        PlatformResourceRequest request = mock(PlatformResourceRequest.class);
        setupMocks(request);
        doAsCurrentUserCrn(() -> underTest.getVmTypesForVerticalScaling(ENV_CRN, null, null, null, Architecture.X86_64.getName()));
        verify(request, times(1)).setFilters(any());
    }

    @Test
    void testGetVmTypesForVerticalScalingWithAvailabilityZonesFilterAsEmpty() {
        PlatformResourceRequest request = mock(PlatformResourceRequest.class);
        setupMocks(request);
        doAsCurrentUserCrn(() -> underTest.getVmTypesForVerticalScaling(ENV_CRN, null, null, List.of(), Architecture.X86_64.getName()));
        verify(request, times(1)).setFilters(any());
    }

    @Test
    void testGetVmTypesForVerticalScalingWithAvailabilityZonesFilterAsNonEmpty() {
        PlatformResourceRequest request = mock(PlatformResourceRequest.class);
        setupMocks(request);
        doAsCurrentUserCrn(() -> underTest.getVmTypesForVerticalScaling(ENV_CRN, null, null, List.of("1", "2", "3"), Architecture.X86_64.getName()));
        verify(request, times(1)).setFilters(Map.of(NetworkConstants.AVAILABILITY_ZONES, "1,2,3"));
    }

    private void setupMocks(PlatformResourceRequest request) {
        Region region = new Region();
        region.setName("west-us2");
        try (MockedStatic<ThreadBasedUserCrnProvider> mockedThreadBasedUserCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedThreadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.getAccountId()).thenReturn(null);
        }
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentDto.getRegions()).thenReturn(Set.of(region));
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);

        when(platformParameterService.getPlatformResourceRequestByEnvironmentForVerticalScaling(ACCOUNT_ID, ENV_CRN, "west-us2",
                        null)).thenReturn(request);
        CloudVmTypes cloudVmTypes = mock(CloudVmTypes.class);
        when(platformParameterService.getVmTypesByCredential(request)).thenReturn(cloudVmTypes);
        when(verticalScaleInstanceProvider.listInstanceTypes(null, null, cloudVmTypes, null,
                null)).thenReturn(cloudVmTypes);
        PlatformVmtypesResponse response = mock(PlatformVmtypesResponse.class);
        when(cloudVmTypesToPlatformVmTypesV1ResponseConverter.convert(cloudVmTypes)).thenReturn(response);
    }

    private <T> T doAsCurrentUserCrn(Supplier<T> callable) {
        return ThreadBasedUserCrnProvider.doAs(CURRENT_USER_CRN, callable);
    }
}
