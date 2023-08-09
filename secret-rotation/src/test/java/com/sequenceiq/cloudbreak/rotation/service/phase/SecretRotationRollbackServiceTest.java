package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationRollbackServiceTest extends AbstractSecretRotationTest {

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @InjectMocks
    private SecretRotationRollbackService underTest;

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.listStepsProgressByRotation(any())).thenReturn(List.of(new SecretRotationStepProgress(null, TEST, STEP, ROTATE, null)));

        underTest.rollback(new RotationMetadata(TEST, ROLLBACK, null, "resource", Optional.empty()));

        verify(contextProvider).getContexts(anyString());
        verify(executor).executeRollback(any(), any());
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
