package com.sequenceiq.cloudbreak.service.secret.service.rotation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@ExtendWith(MockitoExtension.class)
public class VaultRotationExecutorTest {

    @Mock
    private SecretService secretService;

    @InjectMocks
    private VaultRotationExecutor underTest;

    @Test
    public void testVaultRotation() throws Exception {
        when(secretService.putRotation(anyString(), anyString())).thenReturn("anything");
        when(secretService.getRotation(anyString())).thenReturn(new RotationSecret("new", null));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of("secretPath", "secret"))
                .build();
        underTest.rotate(rotationContext);

        verify(secretService, times(1)).putRotation(eq("secretPath"), eq("secret"));
    }

    @Test
    public void testVaultRotationFinalization() throws Exception {
        when(secretService.update(anyString(), anyString())).thenReturn("anything");
        when(secretService.getRotation(anyString())).thenReturn(new RotationSecret("new", "old"));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of("secretPath", "secret"))
                .build();
        underTest.finalize(rotationContext);

        verify(secretService, times(1)).update(eq("secretPath"), eq("new"));
    }

    @Test
    public void testVaultRotationRollback() throws Exception {
        when(secretService.update(anyString(), anyString())).thenReturn("anything");
        when(secretService.getRotation(anyString())).thenReturn(new RotationSecret("new", "old"));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of("secretPath", "secret"))
                .build();
        underTest.rollback(rotationContext);

        verify(secretService, times(1)).update(eq("secretPath"), eq("old"));
    }

    @Test
    public void testVaultRotationFailure() throws Exception {
        when(secretService.putRotation(anyString(), anyString())).thenThrow(new Exception("anything"));
        when(secretService.getRotation(anyString())).thenReturn(new RotationSecret("new", null));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withVaultPathSecretMap(Map.of("secretPath", "secret"))
                .build();
        assertThrows(SecretRotationException.class, () -> underTest.rotate(rotationContext));
    }
}
