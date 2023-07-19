package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationRollbackServiceTest extends AbstractSecretRotationTest {

    @InjectMocks
    private SecretRotationRollbackService underTest;

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any(), any());

        underTest.rollback(new RotationMetadata(TEST, ROLLBACK, null, "resource", Optional.empty()), null);

        verify(contextProvider).getContexts(anyString());
        verify(executor).executeRollback(any(), any());
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
