package com.sequenceiq.environment.environment.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeFlowService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;

@ExtendWith(MockitoExtension.class)
class EnvironmentDefaultComputeClusterControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:userId";

    @Mock
    private EnvironmentApiConverter environmentApiConverter;

    @Mock
    private EnvironmentModificationService environmentModificationService;

    @Mock
    private ExternalizedComputeFlowService externalizedComputeFlowService;

    @InjectMocks
    private EnvironmentDefaultComputeClusterController underTest;

    @Test
    void testCreateDefaultExternalizedComputeCluster() {
        String envCrn = "crn123";
        ExternalizedComputeCreateRequest request = new ExternalizedComputeCreateRequest();
        Environment environment = new Environment();
        when(environmentModificationService.getEnvironment(any(), eq(NameOrCrn.ofCrn(envCrn)))).thenReturn(environment);

        ExternalizedComputeClusterDto dto = ExternalizedComputeClusterDto.builder().build();
        when(environmentApiConverter.requestToExternalizedComputeClusterDto(eq(request), any())).thenReturn(dto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createDefaultExternalizedComputeCluster(envCrn, request, false));
        verify(environmentModificationService, times(1)).getEnvironment(any(), eq(NameOrCrn.ofCrn(envCrn)));
        verify(externalizedComputeFlowService, times(1)).initializeDefaultExternalizedComputeCluster(environment, dto, false);
    }

    @Test
    void testReinitializeDefaultExternalizedComputeCluster() {
        String envCrn = "crn123";
        ExternalizedComputeCreateRequest request = new ExternalizedComputeCreateRequest();
        Environment environment = new Environment();
        when(environmentModificationService.getEnvironment(any(), eq(NameOrCrn.ofCrn(envCrn)))).thenReturn(environment);
        ExternalizedComputeClusterDto dto = ExternalizedComputeClusterDto.builder().build();
        when(environmentApiConverter.requestToExternalizedComputeClusterDto(eq(request), any())).thenReturn(dto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.reinitializeDefaultExternalizedComputeCluster(envCrn, request, true));
        verify(environmentModificationService, times(1)).getEnvironment(any(), eq(NameOrCrn.ofCrn(envCrn)));
        verify(externalizedComputeFlowService, times(1)).initializeDefaultExternalizedComputeCluster(environment, dto, true);
    }

}