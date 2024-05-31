package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.user.DirectoryManagerUserService;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaDirectoryManagerPasswordRotationExecutorTest {

    @Mock
    private DirectoryManagerUserService directoryManagerUserService;

    @Mock
    private StackService stackService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @InjectMocks
    private FreeIpaDirectoryManagerPasswordRotationExecutor rotationExecutor;

    @Test
    void rotateWhenAdminPasswordInRotationStateShouldUpdateDirectoryManagerPassword() throws Exception {
        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn("crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967")
                .withAdminPasswordSecret("admin-password-secret")
                .build();

        RotationSecret rotationSecret = new RotationSecret("new-password", "old-password");
        when(uncachedSecretServiceForRotation.getRotation(eq(rotationContext.getAdminPasswordSecret()))).thenReturn(rotationSecret);

        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(rotationContext.getResourceCrn()), anyString())).thenReturn(stack);
        rotationExecutor.rotate(rotationContext);
        verify(uncachedSecretServiceForRotation).getRotation(eq(rotationContext.getAdminPasswordSecret()));
        verify(directoryManagerUserService).updateDirectoryManagerPassword(eq(stack), eq("new-password"));
    }

    @Test
    void rotateWhenAdminPasswordNotInRotationStateShouldThrowException() {
        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn("crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967")
                .withAdminPasswordSecret("admin-password-secret")
                .build();

        RotationSecret rotationSecret = new RotationSecret("oldSecret", null);
        when(uncachedSecretServiceForRotation.getRotation(eq(rotationContext.getAdminPasswordSecret()))).thenReturn(rotationSecret);

        SecretRotationException exception = assertThrows(SecretRotationException.class, () -> {
            rotationExecutor.rotate(rotationContext);
        });

        assertEquals("Freeipa admin password is not in rotation state in Vault, thus rotation of directory manager password " +
                "is not possible.", exception.getMessage());
    }

    @Test
    void rollbackWhenBackupPasswordExistsShouldUpdateDirectoryManagerPassword() throws Exception {
        FreeIpaAdminPasswordRotationContext rotationContext = FreeIpaAdminPasswordRotationContext.builder()
                .withResourceCrn("crn:cdp:environments:us-west-1:cloudera:environment:ceb2ac9d-16f5-45de-9e9b-d38ed26d6967")
                .withAdminPasswordSecret("admin-password-secret")
                .build();

        RotationSecret rotationSecret = new RotationSecret("null", "backup-password");
        when(uncachedSecretServiceForRotation.getRotation(eq(rotationContext.getAdminPasswordSecret()))).thenReturn(rotationSecret);

        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(rotationContext.getResourceCrn()), anyString())).thenReturn(stack);

        rotationExecutor.rollback(rotationContext);

        verify(uncachedSecretServiceForRotation).getRotation(eq(rotationContext.getAdminPasswordSecret()));
        verify(directoryManagerUserService).checkDirectoryManagerPassword(eq(stack));
    }
}
