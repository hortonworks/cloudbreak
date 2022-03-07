package com.sequenceiq.cloudbreak.service.secret.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

@RunWith(MockitoJUnitRunner.class)
public class SecretAspectServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private SecretService secretService;

    @InjectMocks
    private SecretAspectService underTest;

    @Test
    public void testVaultPutWhenAccountIdDefinedThenMustWriteTheRightPath() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league", "super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        when(secretService.put(keyCaptor.capture(), valueCaptor.capture())).thenReturn("");

        underTest.proceedSave(proceedingJoinPoint);

        Assert.assertTrue(keyCaptor.getValue().startsWith("justice-league/vaulttest/power/"));
        Assert.assertEquals(valueCaptor.getValue(), "super");
    }

    @Test
    public void testVaultPutWhenAccountIdNotImplementedThenShouldThrowIllegalArgumentException() throws Exception {
        VaultWrongTest vaultTest = new VaultWrongTest("super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        thrown.expect(SecretOperationException.class);
        thrown.expectMessage("VaultWrongTest must be a subclass of AccountIdAwareResource");

        underTest.proceedSave(proceedingJoinPoint);
    }

    @Test
    public void testVaultPutWhenSecretServicePutMethodThrowRuntimeExceptionThenShouldThrowSecretOperationException() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league", "super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        when(secretService.put(anyString(), anyString())).thenThrow(new RuntimeException("runtime"));

        thrown.expect(SecretOperationException.class);
        thrown.expectMessage("runtime");

        underTest.proceedSave(proceedingJoinPoint);
    }

    @Test
    public void testVaultDeleteWhenAccountIdDefinedThenMustWriteTheRightPath() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league",
                new Secret("super", "justice-league/vaulttest/power/123-123-123-123"));
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(secretService).delete(keyCaptor.capture());

        underTest.proceedDelete(proceedingJoinPoint);

        Assert.assertTrue(keyCaptor.getValue().startsWith("justice-league/vaulttest/power/123-123-123-123"));
    }

    private static class VaultTestProceedingJoinPoint implements ProceedingJoinPoint {

        private Object[] args;

        VaultTestProceedingJoinPoint(Object obj) {
            this.args = new Object[]{obj};
        }

        @Override
        public void set$AroundClosure(AroundClosure arc) {

        }

        @Override
        public Object proceed() throws Throwable {
            return "GO";
        }

        @Override
        public Object proceed(Object[] args) throws Throwable {
            return "GO";
        }

        @Override
        public String toShortString() {
            return "short";
        }

        @Override
        public String toLongString() {
            return "long";
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public Signature getSignature() {
            return null;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public String getKind() {
            return null;
        }

        @Override
        public StaticPart getStaticPart() {
            return null;
        }
    }

    private static class VaultTest implements AccountIdAwareResource {

        private String accountId;

        @SecretValue
        private Secret power;

        VaultTest(String accountId, String power) {
            this.accountId = accountId;
            this.power = new Secret(power);
        }

        VaultTest(String accountId, Secret power) {
            this.accountId = accountId;
            this.power = power;
        }

        public Secret getPower() {
            return power;
        }

        @Override
        public String getAccountId() {
            return accountId;
        }
    }

    private static class VaultWrongTest {

        @SecretValue
        private Secret power;

        VaultWrongTest(String power) {
            this.power = new Secret(power);
        }

        VaultWrongTest(Secret power) {
            this.power = power;
        }

        public Secret getPower() {
            return power;
        }
    }

}