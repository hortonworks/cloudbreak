package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationPreValidateServiceTest extends AbstractSecretRotationTest {

    @InjectMocks
    private SecretRotationPreValidateService underTest;

    @Test
    public void testPreValidate() {
        doNothing().when(executor).executePreValidation(any());

        underTest.preValidate(new RotationMetadata(TEST, ROTATE, null, "resource", Optional.empty()));

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executePreValidation(any());
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
