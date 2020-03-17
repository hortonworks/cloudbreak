package com.sequenceiq.environment.environment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;

@ExtendWith(MockitoExtension.class)
public class EnvironmentStartServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = "1234";

    private static final String ENV_CRN = "crn:env";

    private static final String PARENT_ENV_CRN = "crn:parent";

    private static final long ENV_ID = 123L;

    private static final String ENV_NAME = "name";

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private EnvironmentStartService underTest;

    @Test
    public void shouldStartEnvironmentWithoutParent() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.setParentEnvironmentCrn(null);
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startByCrn(ENV_CRN));

        verify(reactorFlowManager).triggerStartFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldStartEnvironmentGivenParentIsAvailable() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.setParentEnvironmentCrn(PARENT_ENV_CRN);
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        EnvironmentDto parentEnvironmentDto = new EnvironmentDto();
        parentEnvironmentDto.setStatus(EnvironmentStatus.AVAILABLE);
        when(environmentService.getByCrnAndAccountId(PARENT_ENV_CRN, ACCOUNT_ID)).thenReturn(parentEnvironmentDto);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startByCrn(ENV_CRN));

        verify(reactorFlowManager).triggerStartFlow(ENV_ID, ENV_NAME, USER_CRN);
    }

    @Test
    public void shouldThrowBadRequestExceptionGivenParentIsNotAvailable() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        environmentDto.setParentEnvironmentCrn(PARENT_ENV_CRN);
        when(environmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(environmentDto);
        EnvironmentDto parentEnvironmentDto = new EnvironmentDto();
        parentEnvironmentDto.setName("Parent-env-name");
        parentEnvironmentDto.setStatus(EnvironmentStatus.ENV_STOPPED);
        when(environmentService.getByCrnAndAccountId(PARENT_ENV_CRN, ACCOUNT_ID)).thenReturn(parentEnvironmentDto);

        Assertions.assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startByCrn(ENV_CRN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(parentEnvironmentDto.getName());
    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENV_ID);
        environmentDto.setName(ENV_NAME);
        return environmentDto;
    }

}