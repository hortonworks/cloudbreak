package com.sequenceiq.cloudbreak.aspect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;

@ExtendWith(MockitoExtension.class)
class SecretAspectsTest {

    @InjectMocks
    private SecretAspects underTest;

    @Mock
    private SecretService secretService;

    @Mock
    private Clock clock;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Tenant tenant;

    @Test
    void testproceedSaveEntityNotContainsSecret() {
        when(proceedingJoinPoint.getArgs()).thenReturn(new String[] { "test" });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);
    }

    @Test
    void testproceedSaveEntitySecretIsNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(null);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    void testproceedSaveEntitySecretRawIsNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret(null));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    void testproceedSaveEntitySecretSecretNotNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret(null, ""));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    void testproceedSaveEntityNotTenantAwareResource() throws Exception {
        DummyEntity dummyEntity = new DummyEntity(new Secret(""));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        try {
            underTest.proceedOnRepositorySave(proceedingJoinPoint);
        } catch (Exception e) {
            assertThat(e, IsInstanceOf.instanceOf(CloudbreakServiceException.class));
            assertThat(e.getCause(), IsInstanceOf.instanceOf(IllegalArgumentException.class));
        }

        verifySecretManagementIgnoredDuringSave(null);
    }

    @Test
    void testproceedSaveEntity() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret("raw"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });
        when(tenant.getName()).thenReturn("tenant");

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verify(secretService, times(1)).put(anyString(), eq("raw"));

        assertThat(dummyEntity.secret, IsInstanceOf.instanceOf(SecretProxy.class));
    }

    @Test
    void testproceedSaveAllEntity() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret("raw"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { List.of(dummyEntity) });
        when(tenant.getName()).thenReturn("tenant");

        underTest.proceedOnRepositorySaveAll(proceedingJoinPoint);

        verify(secretService, times(1)).put(anyString(), eq("raw"));

        assertThat(dummyEntity.secret, IsInstanceOf.instanceOf(SecretProxy.class));
    }

    @Test
    void testproceedDeleteEntityNotContainsSecret() {
        when(proceedingJoinPoint.getArgs()).thenReturn(new String[] { "test" });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);
    }

    @Test
    void testproceedDeleteEntityPathIsNull() {
        DummyEntity dummyEntity = new DummyEntity(null);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(0)).deleteByVaultSecretJson(anyString());
    }

    @Test
    void testproceedDeleteEntitySecretIsNull() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, null));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(0)).deleteByVaultSecretJson(anyString());
    }

    @Test
    void testproceedDeleteEntity() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, "path"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(1)).deleteByVaultSecretJson(eq("path"));
    }

    @Test
    void testproceedDeleteAllEntity() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, "path"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { List.of(dummyEntity) });

        underTest.proceedOnRepositoryDeleteAll(proceedingJoinPoint);

        verify(secretService, times(1)).deleteByVaultSecretJson(eq("path"));
    }

    @Test
    void testProceedSaveInCorrectPathWhenAccountIdIsDefined() throws Exception {
        DummyAccountIdAwareResourceEntity dummyEntity = new DummyAccountIdAwareResourceEntity("accountId", "secret");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        when(secretService.put(pathCaptor.capture(), valueCaptor.capture())).thenReturn("");

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        assertTrue(pathCaptor.getValue().startsWith("accountId/dummyaccountidawareresourceentity/secret"));
        assertEquals("secret", valueCaptor.getValue());
    }

    @Test
    void testThrowCloudbreakServiceExceptionWhenAccountIdIsNullOnEntity() throws Exception {
        DummyAccountIdAwareResourceEntity dummyEntity = new DummyAccountIdAwareResourceEntity(null, "secret");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        assertThrows(CloudbreakServiceException.class, () -> underTest.proceedOnRepositorySave(proceedingJoinPoint));
        verifySecretManagementIgnoredDuringSave(dummyEntity.getSecret());
    }

    @Test
    void testThrowCloudbreakServiceExceptionWhenEntityIsNotAccountIdAwareResource() throws Exception {
        DummyEntity dummyEntity = new DummyEntity(new Secret("secret"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        assertThrows(CloudbreakServiceException.class, () -> underTest.proceedOnRepositorySave(proceedingJoinPoint));
        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    void testProceedDeleteInCorrectPathWhenAccountIdIsDefined() {
        DummyAccountIdAwareResourceEntity dummyEntity = new DummyAccountIdAwareResourceEntity("accountId",
                new Secret("secret", "accountId/dummyaccountidawareresourceentity/secret/test-123"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(secretService).deleteByVaultSecretJson(pathCaptor.capture());

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        assertTrue(pathCaptor.getValue().startsWith("accountId/dummyaccountidawareresourceentity/secret/test-123"));
    }

    private void verifySecretManagementIgnoredDuringSave(Secret secret) throws Exception {
        verify(secretService, times(0)).put(anyString(), anyString());

        assertFalse(secret instanceof SecretProxy);
    }

    private static class DummyEntity {
        @SecretValue
        private final Secret secret;

        DummyEntity(Secret secret) {
            this.secret = secret;
        }
    }

    private class DummyTenantAwareResourceEntity implements TenantAwareResource {
        @SecretValue
        private final Secret secret;

        DummyTenantAwareResourceEntity(Secret secret) {
            this.secret = secret;
        }

        @Override
        public Tenant getTenant() {
            return tenant;
        }
    }

    private static class DummyAccountIdAwareResourceEntity implements AccountIdAwareResource {

        private final String accountId;

        @SecretValue
        private final Secret secret;

        DummyAccountIdAwareResourceEntity(String accountId, String secret) {
            this.accountId = accountId;
            this.secret = new Secret(secret);
        }

        DummyAccountIdAwareResourceEntity(String accountId, Secret secret) {
            this.accountId = accountId;
            this.secret = secret;
        }

        public Secret getSecret() {
            return secret;
        }

        public String getRaw() {
            return secret.getRaw();
        }

        @Override
        public String getAccountId() {
            return accountId;
        }
    }
}
