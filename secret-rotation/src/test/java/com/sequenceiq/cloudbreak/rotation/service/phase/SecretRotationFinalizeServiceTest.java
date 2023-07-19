package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationFinalizeServiceTest extends AbstractSecretRotationTest {

    @InjectMocks
    private SecretRotationFinalizeService underTest;

    @Test
    public void testFinalize() {
        doNothing().when(executor).executeFinalize(any(), any());

        underTest.finalize(new RotationMetadata(TEST, FINALIZE, null, "resource", Optional.empty()));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeFinalize(any(), any());
    }

    @Test
    public void testFinalizeIfPostValidateFails() {
        doThrow(new SecretRotationException("anything", null)).when(executor).executePostValidation(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.finalize(new RotationMetadata(TEST, RotationFlowExecutionType.ROTATE, null, "resource", Optional.empty())));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(0)).executeFinalize(any(), any());
        verify(executor, times(1)).executePostValidation(any());
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
