package com.sequenceiq.environment.environment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxStopValidationResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentStopServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = "1234";

    private static final String ENV_CRN = "crn:env";

    private static final long ENV_ID = 123L;

    private static final String ENV_NAME = "name";

    private static final String SDX_CRN = "crn:datalake";

    private static final String SDX_NAME = "sdx_name";

    private static final int FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private EnvironmentStopService underTest;

    @BeforeEach
    public void setup() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.setName(ENV_NAME);
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(Lists.emptyList());
    }

    @Test
    public void shouldStopEnvWithoutChildren() {
        when(sdxService.list(ENV_NAME)).thenReturn(Lists.list());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN));
        verify(reactorFlowManager).triggerStopFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldStopEnvWithOnlyStoppedChildren() {
        when(sdxService.list(ENV_NAME)).thenReturn(Lists.list());

        Environment childEnvironment = new Environment();
        childEnvironment.setStatus(EnvironmentStatus.ENV_STOPPED);
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(List.of(childEnvironment));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN));

        verify(reactorFlowManager).triggerStopFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldStopEnvWithNoUnstoppableSdxCluster() {
        SdxClusterResponse clusterResponse = new SdxClusterResponse();
        clusterResponse.setCrn(SDX_CRN);
        when(sdxService.list(ENV_NAME)).thenReturn(Lists.list(clusterResponse));
        SdxStopValidationResponse sdxStopValidationResponse = new SdxStopValidationResponse(true, null);
        when(sdxService.isStoppable(SDX_CRN)).thenReturn(sdxStopValidationResponse);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN));
        verify(reactorFlowManager).triggerStopFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldThrowBadRequestExceptionGivenEnvHasRunningChildren() {
        Environment childEnvironment = new Environment();
        childEnvironment.setStatus(EnvironmentStatus.AVAILABLE);
        childEnvironment.setName("Child-env-name");
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(List.of(childEnvironment));

        Assertions.assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(childEnvironment.getName());
    }

    @Test
    public void shouldThrowBadRequestExceptionGivenFreeIpaIsRunningOnSpotInstances() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.setFreeIpaCreation(FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(100)
                                .build())
                        .build())
                .build());
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);

        Assertions.assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Environment [name] can not be stopped because FreeIpa is running on spot instances.");
    }

    @Test
    public void shouldThrowBadRequestExceptionGivenCantStopDatalakes() {
        SdxClusterResponse clusterResponse = new SdxClusterResponse();
        clusterResponse.setCrn(SDX_CRN);
        clusterResponse.setName(SDX_NAME);
        when(sdxService.list(ENV_NAME)).thenReturn(Lists.list(clusterResponse));
        SdxStopValidationResponse sdxStopValidationResponse = new SdxStopValidationResponse(false, SDX_NAME + "can't be stopped!");
        when(sdxService.isStoppable(SDX_CRN)).thenReturn(sdxStopValidationResponse);

        Assertions.assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(SDX_NAME);
    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENV_ID);
        environmentDto.setName(ENV_NAME);
        return environmentDto;
    }

}