package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.util.TestConstants.ENV_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@ExtendWith(MockitoExtension.class)
class EnvironmentVerticalScaleServiceTest {

    private static final boolean NOT_ENTITLED_FOR_VERTICAL_SCALE = false;

    private static final boolean ENTITLED_FOR_VERTICAL_SCALE = true;

    private static final String ENV_NAME = "someAwesomeEnv";

    private static final String ACCOUNT = "someAccount";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT + ":user:5678";

    private static final VerificationMode CALLED_ONCE = times(1);

    private static final VerticalScaleRequest TEST_VERTICAL_SCALE_REQUEST = new VerticalScaleRequest();

    private static final String EXCEPTION_MSG_UPON_NOT_GRANTED_ENTITLEMENT = "The account is not entitled for Vertical Scaling.";

    private static final String STATUS_ISSUE_MESSAGE_FORMAT = "Environment '%s' is not in a vertical scalable state. The environment state is %s";

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private EnvironmentReactorFlowManager mockEnvironmentReactorFlowManager;

    @Mock
    private EnvironmentService mockEnvironmentService;

    private EnvironmentVerticalScaleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentVerticalScaleService(mockEnvironmentReactorFlowManager, mockEnvironmentService);
        lenient().when(mockEnvironmentDto.getAccountId()).thenReturn(ACCOUNT);
        lenient().when(mockEnvironmentDto.getName()).thenReturn(ENV_NAME);
        lenient().when(mockEnvironmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT)).thenReturn(mockEnvironmentDto);
        lenient().when(mockEnvironmentService.getByNameAndAccountId(ENV_NAME, ACCOUNT)).thenReturn(mockEnvironmentDto);
    }

    @ParameterizedTest
    @MethodSource("getInvalidStates")
    void testVerticalScaleByCrnWhenStatusIsNotAppropriate(EnvironmentStatus status) {
        when(mockEnvironmentDto.getStatus()).thenReturn(status);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> doAs(USER_CRN, () -> underTest.verticalScaleByCrn(ENV_CRN, TEST_VERTICAL_SCALE_REQUEST)));

        assertEquals(String.format(STATUS_ISSUE_MESSAGE_FORMAT, ENV_NAME, status.name()), exception.getMessage());

        verify(mockEnvironmentService, CALLED_ONCE).getByCrnAndAccountId(ENV_CRN, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verifyNoInteractions(mockEnvironmentReactorFlowManager);
    }

    @ParameterizedTest
    @MethodSource("getValidStates")
    void testVerticalScaleByCrnWhenStatusIsAppropriate(EnvironmentStatus status) {
        when(mockEnvironmentDto.getStatus()).thenReturn(status);

        doAs(USER_CRN, () -> underTest.verticalScaleByCrn(ENV_CRN, TEST_VERTICAL_SCALE_REQUEST));

        verify(mockEnvironmentService, CALLED_ONCE).getByCrnAndAccountId(ENV_CRN, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verify(mockEnvironmentReactorFlowManager, CALLED_ONCE).triggerVerticalScaleFlow(mockEnvironmentDto, USER_CRN, TEST_VERTICAL_SCALE_REQUEST);
        verifyNoMoreInteractions(mockEnvironmentReactorFlowManager);
    }

    @ParameterizedTest
    @MethodSource("getValidStates")
    void testVerticalScaleByNameWhenEntitlementIsGranted(EnvironmentStatus status) {
        when(mockEnvironmentDto.getStatus()).thenReturn(status);

        doAs(USER_CRN, () -> underTest.verticalScaleByName(ENV_NAME, TEST_VERTICAL_SCALE_REQUEST));

        verify(mockEnvironmentService, CALLED_ONCE).getByNameAndAccountId(ENV_NAME, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verify(mockEnvironmentReactorFlowManager, CALLED_ONCE).triggerVerticalScaleFlow(mockEnvironmentDto, USER_CRN, TEST_VERTICAL_SCALE_REQUEST);
        verifyNoMoreInteractions(mockEnvironmentReactorFlowManager);
    }

    @ParameterizedTest
    @MethodSource("getInvalidStates")
    void testVerticalScaleByNameWhenStatusIsNotAppropriate(EnvironmentStatus status) {
        when(mockEnvironmentDto.getStatus()).thenReturn(status);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> doAs(USER_CRN,
                () -> underTest.verticalScaleByName(ENV_NAME, TEST_VERTICAL_SCALE_REQUEST)));

        assertEquals(String.format(STATUS_ISSUE_MESSAGE_FORMAT, ENV_NAME, status.name()), exception.getMessage());

        verify(mockEnvironmentService, CALLED_ONCE).getByNameAndAccountId(ENV_NAME, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verifyNoInteractions(mockEnvironmentReactorFlowManager);
    }

    private static Stream<Arguments> getInvalidStates() {
        return Arrays.asList(EnvironmentStatus.values()).stream().filter(status -> !status.isVerticalScaleAllowed()).map(Arguments::of);
    }

    private static Stream<Arguments> getValidStates() {
        return Arrays.asList(EnvironmentStatus.values()).stream().filter(EnvironmentStatus::isVerticalScaleAllowed).map(Arguments::of);
    }

}