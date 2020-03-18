package com.sequenceiq.environment.environment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@ExtendWith(MockitoExtension.class)
class EnvironmentStopServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = "1234";

    private static final String ENV_CRN = "crn:env";

    private static final long ENV_ID = 123L;

    private static final String ENV_NAME = "name";

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private EnvironmentStopService underTest;

    @Test
    public void shouldStopEnvWithoutChildren() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(Lists.emptyList());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN));

        verify(reactorFlowManager).triggerStopFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldStopEnvWithOnlyStoppedChildren() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        Environment childEnvironment = new Environment();
        childEnvironment.setStatus(EnvironmentStatus.ENV_STOPPED);
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(List.of(childEnvironment));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN));

        verify(reactorFlowManager).triggerStopFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldThrowBadRequestExceptionGivenEnvHasRunningChildren() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        Environment childEnvironment = new Environment();
        childEnvironment.setStatus(EnvironmentStatus.AVAILABLE);
        childEnvironment.setName("Child-env-name");
        when(environmentService.findAllByAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENV_ID)).thenReturn(List.of(childEnvironment));

        Assertions.assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.stopByCrn(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(childEnvironment.getName());
    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENV_ID);
        environmentDto.setName(ENV_NAME);
        return environmentDto;
    }

}