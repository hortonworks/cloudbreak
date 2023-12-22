package com.sequenceiq.freeipa.service.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.rotation.context.FreeIpaUserPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaUserPasswordRotationExecutorTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private StackService stackService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private FreeIpaUserPasswordRotationExecutor underTest;

    @Test
    void testRotate() throws FreeIpaClientException {
        when(secretService.getRotation(any())).thenReturn(new RotationSecret("secret", "backup"));
        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(freeIpaClient);
        User user = new User();
        user.setUid("uid");
        when(freeIpaClient.userShow(any())).thenReturn(user);
        underTest.rotate(FreeIpaUserPasswordRotationContext.builder()
                .withResourceCrn(ENV_CRN)
                .withPasswordSecret("passPath")
                .build());
    }

    @Test
    void testClientCreationDuringRotate() throws FreeIpaClientException {
        when(secretService.getRotation(any())).thenReturn(new RotationSecret("secret", "backup"));
        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenThrow(new FreeIpaClientException("error"));
        assertThrows(SecretRotationException.class, () -> underTest.rotate(FreeIpaUserPasswordRotationContext.builder()
                .withResourceCrn(ENV_CRN)
                .withPasswordSecret("passPath")
                .build()));
    }

    @Test
    void testVaultCorruptionDuringRotate() {
        when(secretService.getRotation(any())).thenReturn(new RotationSecret("secret", null));
        assertThrows(SecretRotationException.class, () -> underTest.rotate(FreeIpaUserPasswordRotationContext.builder()
                .withPasswordSecret("passPath")
                .build()));
    }

    @Test
    void testRollback() throws FreeIpaClientException {
        when(secretService.getRotation(any())).thenReturn(new RotationSecret("secret", "backup"));
        Stack stack = mock(Stack.class);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(freeIpaClient);
        User user = new User();
        user.setUid("uid");
        when(freeIpaClient.userShow(any())).thenReturn(user);
        underTest.rollback(FreeIpaUserPasswordRotationContext.builder()
                .withResourceCrn(ENV_CRN)
                .withPasswordSecret("passPath")
                .build());
    }

    @Test
    void testVaultCorruptionDuringRollback() {
        when(secretService.getRotation(any())).thenReturn(new RotationSecret("secret", null));
        assertThrows(SecretRotationException.class, () -> underTest.rollback(FreeIpaUserPasswordRotationContext.builder()
                .withPasswordSecret("passPath")
                .build()));
    }
}
