package com.sequenceiq.cloudbreak.rotation.service.usage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.usage.service.SecretRotationUsageSenderService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationUsageServiceTest {

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String REASON = "reason";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private SecretRotationUsageSenderService secretRotationUsageSenderService;

    @InjectMocks
    private SecretRotationUsageService underTest;

    @Test
    public void testRotationStartedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rotationStarted(null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationFinishedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rotationFinished(null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationFailedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rotationFailed(null, null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackStartedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rollbackStarted(null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackFinishedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rollbackFinished(null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackFailedWhenErrorOccurs() {
        assertThrows(NullPointerException.class, () -> underTest.rollbackFailed(null, null, null, null));
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationStartedWhenExecutionTypeIsNotNull() {
        underTest.rotationStarted(SECRET_TYPE, RESOURCE_CRN, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationFinishedWhenExecutionTypeIsNotNull() {
        underTest.rotationFinished(SECRET_TYPE, RESOURCE_CRN, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationFailedWhenExecutionTypeIsNotNull() {
        underTest.rotationFailed(SECRET_TYPE, RESOURCE_CRN, null, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackStartedWhenExecutionTypeIsNotNull() {
        underTest.rollbackStarted(SECRET_TYPE, RESOURCE_CRN, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackFinishedWhenExecutionTypeIsNotNull() {
        underTest.rollbackFinished(SECRET_TYPE, RESOURCE_CRN, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRollbackFailedWhenExecutionTypeIsNotNull() {
        underTest.rollbackFailed(SECRET_TYPE, RESOURCE_CRN, null, RotationFlowExecutionType.ROTATE);
        verifyNoInteractions(secretRotationUsageSenderService);
    }

    @Test
    public void testRotationStarted() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationStarted(SECRET_TYPE, RESOURCE_CRN, null));
        verify(secretRotationUsageSenderService).rotationStarted(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN));
    }

    @Test
    public void testRotationFinished() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationFinished(SECRET_TYPE, RESOURCE_CRN, null));
        verify(secretRotationUsageSenderService).rotationFinished(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN));
    }

    @Test
    public void testRotationFailed() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationFailed(SECRET_TYPE, RESOURCE_CRN, REASON, null));
        verify(secretRotationUsageSenderService).rotationFailed(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN), eq(REASON));
    }

    @Test
    public void testRollbackStarted() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackStarted(SECRET_TYPE, RESOURCE_CRN, null));
        verify(secretRotationUsageSenderService).rollbackStarted(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN));
    }

    @Test
    public void testRollbackFinished() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackFinished(SECRET_TYPE, RESOURCE_CRN, null));
        verify(secretRotationUsageSenderService).rollbackFinished(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN));
    }

    @Test
    public void testRollbackFailed() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackFailed(SECRET_TYPE, RESOURCE_CRN, REASON, null));
        verify(secretRotationUsageSenderService).rollbackFailed(eq(SECRET_TYPE.toString()), eq(RESOURCE_CRN), eq(REASON));
    }
}