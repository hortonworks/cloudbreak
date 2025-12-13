package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

@ExtendWith(MockitoExtension.class)
class SecretAspectServiceTest {

    @Mock
    private SecretService secretService;

    @InjectMocks
    private SecretAspectService underTest;

    @Test
    void testVaultPutWhenAccountIdDefinedThenMustWriteTheRightPath() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league", "super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        when(secretService.put(keyCaptor.capture(), valueCaptor.capture())).thenReturn("");

        underTest.proceedSave(proceedingJoinPoint);

        assertTrue(keyCaptor.getValue().startsWith("justice-league/vaulttest/power/"));
        assertEquals(valueCaptor.getValue(), "super");
    }

    @Test
    void testVaultPutWhenAccountIdNotImplementedThenShouldThrowIllegalArgumentException() throws Exception {
        VaultWrongTest vaultTest = new VaultWrongTest("super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        assertThrows(SecretOperationException.class, () -> underTest.proceedSave(proceedingJoinPoint),
                "VaultWrongTest must be a subclass of AccountIdAwareResource");
    }

    @Test
    void testVaultPutWhenSecretServicePutMethodThrowRuntimeExceptionThenShouldThrowSecretOperationException() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league", "super");
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        when(secretService.put(anyString(), anyString())).thenThrow(new RuntimeException("runtime"));

        assertThrows(SecretOperationException.class, () -> underTest.proceedSave(proceedingJoinPoint), "runtime");
    }

    @Test
    void testVaultDeleteWhenAccountIdDefinedThenMustWriteTheRightPath() throws Exception {
        VaultTest vaultTest = new VaultTest("justice-league",
                new Secret("super", "justice-league/vaulttest/power/123-123-123-123"));
        VaultTestProceedingJoinPoint proceedingJoinPoint = new VaultTestProceedingJoinPoint(vaultTest);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        doNothing().when(secretService).deleteByVaultSecretJson(keyCaptor.capture());

        underTest.proceedDelete(proceedingJoinPoint);

        assertTrue(keyCaptor.getValue().startsWith("justice-league/vaulttest/power/123-123-123-123"));
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