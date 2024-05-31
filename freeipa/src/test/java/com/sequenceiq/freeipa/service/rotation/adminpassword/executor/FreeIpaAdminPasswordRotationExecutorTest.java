package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.AdminUserService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaAdminPasswordRotationExecutorTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private StackService stackService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @InjectMocks
    private FreeIpaAdminPasswordRotationExecutor rotationExecutor;

    @Test
    void rotateShouldUpdateAdminUserPasswordWhenInRotationState() throws Exception {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, oldPassword);
        Stack stack = mock(Stack.class);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(eq(stack))).thenReturn(freeIpaClient);
        rotationExecutor.rotate(rotationContext);
        verify(adminUserService, times(1)).updateAdminUserPassword(eq(newPassword), eq(freeIpaClient));
        verify(adminUserService, times(1)).waitAdminUserPasswordReplication(eq(stack));
    }

    @Test
    void rotateShouldThrowExceptionWhenNotInRotationState() {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, null);
        Stack stack = mock(Stack.class);

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);

        SecretRotationException e = assertThrows(SecretRotationException.class,
                () -> rotationExecutor.rotate(rotationContext));
        assertEquals("Freeipa admin password is not in rotation state in Vault, thus rotation of admin password is not possible.",
                e.getMessage());
    }

    @Test
    void rotateShouldThrowExceptionWhenUpdateAdminUserPasswordFails() throws Exception {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, oldPassword);
        Stack stack = mock(Stack.class);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(eq(stack))).thenReturn(freeIpaClient);
        doThrow(new CloudbreakRuntimeException("error")).when(adminUserService).updateAdminUserPassword(newPassword, freeIpaClient);

        SecretRotationException e = assertThrows(SecretRotationException.class, () -> rotationExecutor.rotate(rotationContext));
        assertEquals("error", e.getCause().getMessage());
        verify(adminUserService, never()).waitAdminUserPasswordReplication(eq(stack));
    }

    @Test
    void rollbackShouldSkipRollbackWhenAbleToCreateClientWithBackupSecret() throws Exception {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, oldPassword);
        Stack stack = mock(Stack.class);

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(mock(FreeIpaClient.class));
        // Act
        rotationExecutor.rollback(rotationContext);

        // Assert
        verify(freeIpaClientFactory, times(1)).getFreeIpaClientForStack(eq(stack));
        verify(adminUserService, never()).updateAdminUserPassword(eq(oldPassword), any(FreeIpaClient.class));
        verify(adminUserService, never()).waitAdminUserPasswordReplication(eq(stack));
    }

    @Test
    void rollbackShouldRollbackWhenUnableToCreateClientWithBackupSecret() throws Exception {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, oldPassword);
        Stack stack = mock(Stack.class);
        FreeIpaClientException exception = new FreeIpaClientException("Error");

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        doThrow(exception).doReturn(freeIpaClient).when(freeIpaClientFactory).getFreeIpaClientForStack(eq(stack));
        rotationExecutor.rollback(rotationContext);
        verify(adminUserService, times(1)).updateAdminUserPassword(oldPassword, freeIpaClient);
        verify(adminUserService, times(1)).waitAdminUserPasswordReplication(eq(stack));
    }

    @Test
    void rollbackShouldRollbackWhenUnableToCreateClientWithBackupSecretButFail() throws Exception {
        String resourceCrn = "crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String adminPasswordSecret = "adminPasswordSecret";

        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withAdminPasswordSecret(adminPasswordSecret)
                .build();
        RotationSecret rotationSecret = new RotationSecret(newPassword, oldPassword);
        Stack stack = mock(Stack.class);
        FreeIpaClientException exception = new FreeIpaClientException("Error");

        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(resourceCrn), anyString())).thenReturn(stack);
        when(uncachedSecretServiceForRotation.getRotation(eq(adminPasswordSecret))).thenReturn(rotationSecret);
        doThrow(exception).when(freeIpaClientFactory).getFreeIpaClientForStack(eq(stack));

        CloudbreakRuntimeException e = assertThrows(CloudbreakRuntimeException.class,
                () -> rotationExecutor.rollback(rotationContext));
        assertEquals("The attempt to revert the rotation has been unsuccessful. We are unable to create a client using either " +
                "the new password or the old password.", e.getMessage());
    }
}
