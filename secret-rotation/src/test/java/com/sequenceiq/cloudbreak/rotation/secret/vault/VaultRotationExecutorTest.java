package com.sequenceiq.cloudbreak.rotation.secret.vault;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;

@ExtendWith(MockitoExtension.class)
public class VaultRotationExecutorTest {

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private SecretRotationNotificationService notificationService;

    @InjectMocks
    private VaultRotationExecutor underTest;

    @Test
    public void testPreValidation() {
        when(uncachedSecretServiceForRotation.isSecret(any())).thenReturn(Boolean.TRUE);

        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        underTest.executePreValidation(rotationContext, null);

        verify(uncachedSecretServiceForRotation).isSecret(any());
    }

    @Test
    public void testPreValidationIfSecretInvalid() {
        when(uncachedSecretServiceForRotation.isSecret(any())).thenReturn(Boolean.FALSE);

        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(rotationContext, null));

        verify(uncachedSecretServiceForRotation).isSecret(any());
    }

    @Test
    public void testPostValidation() {
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", "old"));

        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        underTest.executePostValidation(rotationContext, null);

        verify(uncachedSecretServiceForRotation).getRotation(any());
    }

    @Test
    public void testPostValidationIfRotationCorrupted() {
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", null));

        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext, null));

        verify(uncachedSecretServiceForRotation).getRotation(any());
    }

    @Test
    public void testVaultRotation() throws Exception {
        when(uncachedSecretServiceForRotation.putRotation(anyString(), anyString())).thenReturn("anything");
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", null));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            when(sampleRepo.getEntityClass()).thenReturn(Sample.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            underTest.executeRotate(rotationContext, RotationMetadataTestUtil.metadataForRotation("resource", null));

            verify(uncachedSecretServiceForRotation, times(1)).putRotation(eq("secretPath"), eq("new"));
        }
    }

    @Test
    public void testVaultRotationFinalization() throws Exception {
        when(uncachedSecretServiceForRotation.update(anyString(), anyString())).thenReturn("anything");
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", "old"));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            when(sampleRepo.getEntityClass()).thenReturn(Sample.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            underTest.executeFinalize(rotationContext, RotationMetadataTestUtil.metadataForFinalize("resource", null));

            verify(uncachedSecretServiceForRotation, times(1)).update(eq("secretPath"), eq("new"));
        }
    }

    @Test
    public void testVaultRotationRollback() throws Exception {
        when(uncachedSecretServiceForRotation.update(anyString(), anyString())).thenReturn("anything");
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", "old"));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            when(sampleRepo.getEntityClass()).thenReturn(Sample.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            underTest.executeRollback(rotationContext, RotationMetadataTestUtil.metadataForRollback("resource", null));

            verify(uncachedSecretServiceForRotation, times(1)).update(eq("secretPath"), eq("old"));
        }
    }

    @Test
    public void testVaultRotationFailure() throws Exception {
        when(uncachedSecretServiceForRotation.putRotation(anyString(), anyString())).thenThrow(new CloudbreakServiceException("anything"));
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenReturn(new RotationSecret("new", null));
        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withNewSecretMap(Map.of(new Sample(), Map.of(SecretMarker.DP_CLUSTER_MANAGER_USER, "new")))
                .build();
        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(rotationContext,
                RotationMetadataTestUtil.metadataForRotation("resource", null)));
    }

    private class Sample {

        @SecretGetter(marker = SecretMarker.DP_CLUSTER_MANAGER_USER)
        public String getSecret() {
            return "secretPath";
        }

        @SecretSetter(marker = SecretMarker.DP_CLUSTER_MANAGER_USER)
        public void setSecret(Secret secret) {

        }
    }

    private interface SampleRepo extends VaultRotationAwareRepository, CrudRepository<Sample, Long> {

        @Override
        default Class<Sample> getEntityClass() {
            return Sample.class;
        }
    }
}
