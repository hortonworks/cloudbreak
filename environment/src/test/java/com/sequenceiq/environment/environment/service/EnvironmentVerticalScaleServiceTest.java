package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.util.TestConstants.ENV_CRN;
import static com.sequenceiq.cloudbreak.util.TestConstants.ENV_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@ExtendWith(MockitoExtension.class)
class EnvironmentVerticalScaleServiceTest {

    private static final boolean NOT_ENTITLED_FOR_VERTICAL_SCALE = false;

    private static final boolean ENTITLED_FOR_VERTICAL_SCALE = true;

    private static final String ACCOUNT = "someAccount";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT + ":user:5678";

    private static final VerificationMode CALLED_ONCE = times(1);

    private static final VerticalScaleRequest TEST_VERTICAL_SCALE_REQUEST = new VerticalScaleRequest();

    private static final String EXCEPTION_MSG_UPON_NOT_GRANTED_ENTITLEMENT = "The account is not entitled for Vertical Scaling.";

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private EnvironmentReactorFlowManager mockEnvironmentReactorFlowManager;

    @Mock
    private EnvironmentService mockEnvironmentService;

    @Mock
    private EntitlementService mockEntitlementService;

    private EnvironmentVerticalScaleService underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentVerticalScaleService(mockEnvironmentReactorFlowManager, mockEnvironmentService, mockEntitlementService);
        when(mockEnvironmentDto.getAccountId()).thenReturn(ACCOUNT);
        lenient().when(mockEnvironmentService.getByCrnAndAccountId(ENV_CRN, ACCOUNT)).thenReturn(mockEnvironmentDto);
        lenient().when(mockEnvironmentService.getByNameAndAccountId(ENV_NAME, ACCOUNT)).thenReturn(mockEnvironmentDto);
    }

    @Test
    void testVerticalScaleByCrnWhenEntitlementIsNotGranted() {
        when(mockEntitlementService.awsVerticalScaleEnabled(ACCOUNT)).thenReturn(NOT_ENTITLED_FOR_VERTICAL_SCALE);

        BadRequestException expectedException = assertThrows(BadRequestException.class,
                () -> doAs(USER_CRN, () -> underTest.verticalScaleByCrn(ENV_CRN, TEST_VERTICAL_SCALE_REQUEST)));

        assertEquals(EXCEPTION_MSG_UPON_NOT_GRANTED_ENTITLEMENT, expectedException.getMessage());

        verify(mockEntitlementService, CALLED_ONCE).awsVerticalScaleEnabled(ACCOUNT);
        verifyNoMoreInteractions(mockEntitlementService);
        verify(mockEnvironmentService, CALLED_ONCE).getByCrnAndAccountId(ENV_CRN, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verifyNoInteractions(mockEnvironmentReactorFlowManager);
    }

    @Test
    void testVerticalScaleByNameWhenEntitlementIsNotGranted() {
        when(mockEntitlementService.awsVerticalScaleEnabled(ACCOUNT)).thenReturn(NOT_ENTITLED_FOR_VERTICAL_SCALE);

        BadRequestException expectedException = assertThrows(BadRequestException.class,
                () -> doAs(USER_CRN, () -> underTest.verticalScaleByName(ENV_NAME, TEST_VERTICAL_SCALE_REQUEST)));

        assertEquals(EXCEPTION_MSG_UPON_NOT_GRANTED_ENTITLEMENT, expectedException.getMessage());

        verify(mockEntitlementService, CALLED_ONCE).awsVerticalScaleEnabled(ACCOUNT);
        verifyNoMoreInteractions(mockEntitlementService);
        verify(mockEnvironmentService, CALLED_ONCE).getByNameAndAccountId(ENV_NAME, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verifyNoInteractions(mockEnvironmentReactorFlowManager);
    }

    @Test
    void testVerticalScaleByCrnWhenEntitlementIsGranted() {
        when(mockEntitlementService.awsVerticalScaleEnabled(ACCOUNT)).thenReturn(ENTITLED_FOR_VERTICAL_SCALE);

        doAs(USER_CRN, () -> underTest.verticalScaleByCrn(ENV_CRN, TEST_VERTICAL_SCALE_REQUEST));

        verify(mockEntitlementService, CALLED_ONCE).awsVerticalScaleEnabled(ACCOUNT);
        verifyNoMoreInteractions(mockEntitlementService);
        verify(mockEnvironmentService, CALLED_ONCE).getByCrnAndAccountId(ENV_CRN, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verify(mockEnvironmentReactorFlowManager, CALLED_ONCE).triggerVerticalScaleFlow(mockEnvironmentDto, USER_CRN, TEST_VERTICAL_SCALE_REQUEST);
        verifyNoMoreInteractions(mockEnvironmentReactorFlowManager);
    }

    @Test
    void testVerticalScaleByNameWhenEntitlementIsGranted() {
        when(mockEntitlementService.awsVerticalScaleEnabled(ACCOUNT)).thenReturn(ENTITLED_FOR_VERTICAL_SCALE);

        doAs(USER_CRN, () -> underTest.verticalScaleByName(ENV_NAME, TEST_VERTICAL_SCALE_REQUEST));

        verify(mockEntitlementService, CALLED_ONCE).awsVerticalScaleEnabled(ACCOUNT);
        verifyNoMoreInteractions(mockEntitlementService);
        verify(mockEnvironmentService, CALLED_ONCE).getByNameAndAccountId(ENV_NAME, ACCOUNT);
        verifyNoMoreInteractions(mockEnvironmentService);
        verify(mockEnvironmentReactorFlowManager, CALLED_ONCE).triggerVerticalScaleFlow(mockEnvironmentDto, USER_CRN, TEST_VERTICAL_SCALE_REQUEST);
        verifyNoMoreInteractions(mockEnvironmentReactorFlowManager);
    }

}