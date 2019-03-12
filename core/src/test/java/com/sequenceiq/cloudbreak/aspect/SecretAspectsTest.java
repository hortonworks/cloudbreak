package com.sequenceiq.cloudbreak.aspect;

import static org.hamcrest.Matchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SecretProxy;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.TenantAwareResource;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class SecretAspectsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

    @Before
    public void setup() {

    }

    @Test
    public void testproceedSaveEntityNotContainsSecret() {
        when(proceedingJoinPoint.getArgs()).thenReturn(new String[] { "test" });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);
    }

    @Test
    public void testproceedSaveEntitySecretIsNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(null);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    public void testproceedSaveEntitySecretRawIsNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret(null));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    public void testproceedSaveEntitySecretSecretNotNull() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret(null, ""));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(dummyEntity.secret);
    }

    @Test
    public void testproceedSaveEntityNotTenantAwareResource() throws Exception {
        DummyEntity dummyEntity = new DummyEntity(new Secret(""));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        thrown.expect(CloudbreakServiceException.class);
        thrown.expectCause(any(IllegalArgumentException.class));

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verifySecretManagementIgnoredDuringSave(null);
    }

    @Test
    public void testproceedSaveEntity() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret("raw"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });
        when(tenant.getName()).thenReturn("tenant");

        underTest.proceedOnRepositorySave(proceedingJoinPoint);

        verify(secretService, times(1)).put(anyString(), eq("raw"));

        Assert.assertTrue(dummyEntity.secret instanceof SecretProxy);
    }

    @Test
    public void testproceedSaveAllEntity() throws Exception {
        DummyTenantAwareResourceEntity dummyEntity = new DummyTenantAwareResourceEntity(new Secret("raw"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { List.of(dummyEntity) });
        when(tenant.getName()).thenReturn("tenant");

        underTest.proceedOnRepositorySaveAll(proceedingJoinPoint);

        verify(secretService, times(1)).put(anyString(), eq("raw"));

        Assert.assertTrue(dummyEntity.secret instanceof SecretProxy);
    }

    @Test
    public void testproceedDeleteEntityNotContainsSecret() {
        when(proceedingJoinPoint.getArgs()).thenReturn(new String[] { "test" });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);
    }

    @Test
    public void testproceedDeleteEntityPathIsNull() {
        DummyEntity dummyEntity = new DummyEntity(null);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(0)).delete(anyString());
    }

    @Test
    public void testproceedDeleteEntitySecretIsNull() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, null));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(0)).delete(anyString());
    }

    @Test
    public void testproceedDeleteEntity() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, "path"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { dummyEntity });

        underTest.proceedOnRepositoryDelete(proceedingJoinPoint);

        verify(secretService, times(1)).delete(eq("path"));
    }

    @Test
    public void testproceedDeleteAllEntity() {
        DummyEntity dummyEntity = new DummyEntity(new Secret(null, "path"));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { List.of(dummyEntity) });

        underTest.proceedOnRepositoryDeleteAll(proceedingJoinPoint);

        verify(secretService, times(1)).delete(eq("path"));
    }

    private void verifySecretManagementIgnoredDuringSave(Secret secret) throws Exception {
        verify(secretService, times(0)).put(anyString(), anyString());

        Assert.assertFalse(secret instanceof SecretProxy);
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
}
