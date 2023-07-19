package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationServiceTest extends AbstractSecretRotationTest {

    @InjectMocks
    private SecretRotationService underTest;

    @Test
    public void testRotateWhenContextMissing() {
        when(contextProvider.getContexts(anyString())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () ->
                underTest.rotate(new RotationMetadata(TEST, ROTATE, null, "resource", Optional.empty())));

        verify(contextProvider).getContexts(anyString());
        verifyNoInteractions(executor);
    }

    @Test
    public void testRotate() {
        doNothing().when(executor).executeRotate(any(), any());

        underTest.rotate(new RotationMetadata(TEST, ROTATE, null, "resource", Optional.empty()));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any(), any());
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
