package com.sequenceiq.cloudbreak.usage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
public class SecretRotationUsageSenderServiceTest {

    private static final String SECRET_TYPE = "secretType";

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String REASON = "reason";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private SecretRotationUsageSenderService underTest;

    @Test
    public void testRotationStartedWhenErrorOccurs() {
        underTest.rotationStarted(null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationFinishedWhenErrorOccurs() {
        underTest.rotationFinished(null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationFailedWhenErrorOccurs() {
        underTest.rotationFailed(null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackStartedWhenErrorOccurs() {
        underTest.rollbackStarted(null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackFinishedWhenErrorOccurs() {
        underTest.rollbackFinished(null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackFailedWhenErrorOccurs() {
        underTest.rollbackFailed(null, null, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationStartedWhenExecutionTypeIsNotNull() {
        underTest.rotationStarted(SECRET_TYPE, RESOURCE_CRN);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationFinishedWhenExecutionTypeIsNotNull() {
        underTest.rotationFinished(SECRET_TYPE, RESOURCE_CRN);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationFailedWhenExecutionTypeIsNotNull() {
        underTest.rotationFailed(SECRET_TYPE, RESOURCE_CRN, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackStartedWhenExecutionTypeIsNotNull() {
        underTest.rollbackStarted(SECRET_TYPE, RESOURCE_CRN);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackFinishedWhenExecutionTypeIsNotNull() {
        underTest.rollbackFinished(SECRET_TYPE, RESOURCE_CRN);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRollbackFailedWhenExecutionTypeIsNotNull() {
        underTest.rollbackFailed(SECRET_TYPE, RESOURCE_CRN, null);
        verifyNoInteractions(usageReporter);
    }

    @Test
    public void testRotationStarted() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationStarted(SECRET_TYPE, RESOURCE_CRN));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals("", event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.STARTED, event.getStatus());
    }

    @Test
    public void testRotationFinished() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationFinished(SECRET_TYPE, RESOURCE_CRN));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals("", event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.FINISHED, event.getStatus());
    }

    @Test
    public void testRotationFailed() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotationFailed(SECRET_TYPE, RESOURCE_CRN, REASON));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals(REASON, event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.FAILED, event.getStatus());
    }

    @Test
    public void testRollbackStarted() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackStarted(SECRET_TYPE, RESOURCE_CRN));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals("", event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.ROLLBACK_STARTED, event.getStatus());
    }

    @Test
    public void testRollbackFinished() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackFinished(SECRET_TYPE, RESOURCE_CRN));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals("", event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.ROLLBACK_FINISHED, event.getStatus());
    }

    @Test
    public void testRollbackFailed() {
        doNothing().when(usageReporter).cdpSecretRotationEvent(any());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rollbackFailed(SECRET_TYPE, RESOURCE_CRN, REASON));

        ArgumentCaptor<UsageProto.CDPSecretRotationEvent> eventCaptor = ArgumentCaptor.forClass(UsageProto.CDPSecretRotationEvent.class);
        verify(usageReporter).cdpSecretRotationEvent(eventCaptor.capture());
        UsageProto.CDPSecretRotationEvent event = eventCaptor.getValue();
        assertEquals(RESOURCE_CRN, event.getResourceCrn());
        assertEquals(SECRET_TYPE, event.getSecretType());
        assertEquals(REASON, event.getReason());
        assertEquals(UsageProto.CDPSecretRotationStatus.Value.ROLLBACK_FAILED, event.getStatus());
    }
}