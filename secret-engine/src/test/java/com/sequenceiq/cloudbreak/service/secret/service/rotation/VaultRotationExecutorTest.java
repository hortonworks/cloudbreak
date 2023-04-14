package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@ExtendWith(MockitoExtension.class)
public class VaultRotationExecutorTest {

    @Mock
    private SecretService secretService;

    @InjectMocks
    private VaultRotationExecutor underTest;

    @Test
    public void testVaultRotation() throws Exception {
        when(secretService.update(any(), anyString())).thenReturn("anything");
        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(Map.of("user", "password"))
                .build();
        underTest.rotate(rotationContext);

        verify(secretService, times(1)).update(eq("password"), anyString());
    }

    @Test
    public void testVaultRotationFailure() throws Exception {
        when(secretService.update(any(), anyString())).thenThrow(new Exception("anything"));
        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(Map.of("user", "password"))
                .build();
        Assert.assertThrows(SecretRotationException.class, () -> underTest.rotate(rotationContext));
    }
}
