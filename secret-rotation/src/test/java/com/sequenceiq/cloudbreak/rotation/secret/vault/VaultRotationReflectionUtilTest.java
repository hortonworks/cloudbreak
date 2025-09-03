package com.sequenceiq.cloudbreak.rotation.secret.vault;

import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.CB_CLUSTER_MANAGER_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.CB_CLUSTER_MANAGER_USER;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.DP_CLUSTER_MANAGER_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.DP_CLUSTER_MANAGER_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

public class VaultRotationReflectionUtilTest {

    private Object entity;

    @Test
    void testGetVaultJsonByStringField() {
        String result = VaultRotationReflectionUtil.getVaultSecretJson(new Sample(), DP_CLUSTER_MANAGER_USER);
        assertEquals("secret", result);
    }

    @Test
    void testGetVaultJsonBySecretField() {
        String result = VaultRotationReflectionUtil.getVaultSecretJson(new Sample(), DP_CLUSTER_MANAGER_PASSWORD);
        assertEquals("secret", result);
    }

    @Test
    void testGetVaultJsonByInvalidField() {
        assertThrows(SecretRotationException.class,
                () -> VaultRotationReflectionUtil.getVaultSecretJson(new Sample(), CB_CLUSTER_MANAGER_USER),
                "Secret get method is not annotated correctly, cannot extract secret json.");
    }

    @Test
    void testGetVaultJsonWhenAnnotationMissing() {
        assertThrows(SecretRotationException.class, () -> VaultRotationReflectionUtil.getVaultSecretJson(new Sample(), CB_CLUSTER_MANAGER_PASSWORD),
                "Failed to look up for vault secret by secret marker");
    }

    @Test
    void testSetNewSecret() {
        Sample sample = new Sample();
        VaultRotationReflectionUtil.setNewSecret(sample, DP_CLUSTER_MANAGER_USER, new Secret("newRaw", "newSecret"));
        assertEquals("newSecret", sample.getSecret().getSecret());
    }

    @Test
    void testNewSecretWhenAnnotationMissing() {
        assertThrows(SecretRotationException.class,
                () -> VaultRotationReflectionUtil.setNewSecret(new Sample(), DP_CLUSTER_MANAGER_PASSWORD, new Secret("newRaw", "newSecret")));
    }

    @Test
    void testSaveEntity() {
        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            when(sampleRepo.getEntityClass()).thenReturn(Sample.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            VaultRotationReflectionUtil.saveEntity(new Sample());

            verify(sampleRepo).save(any());
        }
    }

    @Test
    void testSaveEntityWhenRepoIsIncorrect() {
        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(mock(JpaRepository.class)));

            assertThrows(SecretRotationException.class, () -> VaultRotationReflectionUtil.saveEntity(new Sample()),
                    "Failed to look up JPA repository for entity class");
        }
    }

    private class Sample {

        private Secret secret;

        @SecretGetter(marker = DP_CLUSTER_MANAGER_USER)
        public String getDpMgrUser() {
            return "secret";
        }

        @SecretGetter(marker = DP_CLUSTER_MANAGER_PASSWORD)
        public Secret getDpMgrPass() {
            return new Secret("raw", "secret");
        }

        @SecretGetter(marker = CB_CLUSTER_MANAGER_USER)
        public Integer getCbMGrUser() {
            return 1;
        }

        @SecretSetter(marker = DP_CLUSTER_MANAGER_USER)
        public void setDpMgrUser(Secret secret) {
            this.secret = secret;
        }

        public Secret getSecret() {
            return secret;
        }
    }

    private interface SampleRepo extends VaultRotationAwareRepository, CrudRepository<Sample, Long> {

        @Override
        default Class<Sample> getEntityClass() {
            return Sample.class;
        }
    }
}
