package com.sequenceiq.cloudbreak.rotation.secret.vault;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecretConverter;

@ExtendWith(MockitoExtension.class)
public class SyncSecretVersionServiceTest {

    @Mock
    private VaultSecretConverter vaultSecretConverter;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private SyncSecretVersionService underTest;

    @Test
    void testNoUpdate() {
        when(vaultSecretConverter.convert(any())).thenCallRealMethod();
        when(secretService.getVersion(any())).thenReturn(Optional.of(1));

        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            underTest.updateEntityIfNeeded("crn", new SampleEntity(), Set.of(SecretMarker.DP_CLUSTER_MANAGER_USER));

            verify(secretService).getVersion(anyString());
            verifyNoInteractions(sampleRepo);
        }
    }

    @Test
    void testUpdate() {
        when(vaultSecretConverter.convert(any())).thenCallRealMethod();
        when(secretService.getVersion(any())).thenReturn(Optional.of(2));

        try (MockedStatic<StaticApplicationContext> appContext = Mockito.mockStatic(StaticApplicationContext.class)) {
            SampleRepo sampleRepo = mock(SampleRepo.class);
            when(sampleRepo.getEntityClass()).thenReturn(SampleEntity.class);
            appContext.when(() -> StaticApplicationContext.getAllMatchingBeans(any())).thenReturn(List.of(sampleRepo));

            underTest.updateEntityIfNeeded("crn", new SampleEntity(), Set.of(SecretMarker.DP_CLUSTER_MANAGER_USER));

            verify(secretService).getVersion(anyString());
            verify(sampleRepo).save(any());
        }
    }

    private class SampleEntity {

        @SecretGetter(marker = SecretMarker.DP_CLUSTER_MANAGER_USER)
        public Secret get() {
            return new SecretProxy("{\"enginePath\":\"secret\",\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                    "\"path\":\"cb/shared/cloudera/cluster/cloudbreakclustermanageruser/5b1fe649-940c-417c-8680-7f9247447c44-19a0ca5db96\",\"version\":1}");
        }

        @SecretSetter(marker = SecretMarker.DP_CLUSTER_MANAGER_USER)
        public void set(Secret secret) {

        }
    }

    private interface SampleRepo extends CrudRepository<SampleEntity, Long>, VaultRotationAwareRepository {

        @Override
        default Class<SampleEntity> getEntityClass() {
            return SampleEntity.class;
        }
    }
}
