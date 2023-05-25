package com.sequenceiq.cloudbreak.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;

@ExtendWith(MockitoExtension.class)
class StackSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String STATUS_REASON = "reason";

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private StackSecretRotationStatusService underTest;

    @Test
    void testRotationStarted() {
        underTest.rotationStarted(RESOURCE_CRN);
        Mockito.verify(stackUpdater, Mockito.times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(DetailedStackStatus.SECRET_ROTATION_STARTED), isNull());
    }

    @Test
    void testRotationFinished() {
        underTest.rotationFinished(RESOURCE_CRN);
        Mockito.verify(stackUpdater, Mockito.times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(DetailedStackStatus.SECRET_ROTATION_FINISHED), isNull());
    }

    @Test
    void testRotationFailed() {
        underTest.rotationFailed(RESOURCE_CRN, STATUS_REASON);
        Mockito.verify(stackUpdater, Mockito.times(1)).updateStackStatus(eq(RESOURCE_CRN), eq(DetailedStackStatus.SECRET_ROTATION_FAILED), eq(STATUS_REASON));
    }

}