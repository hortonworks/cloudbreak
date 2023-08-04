package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory.ADMIN_USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService underTest;

    @Test
    public void testUpdateAdminUserPasswordUpdatePasswordPolicy() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbminpwdlife(10);
        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);
        String newPassword = "newpassword";
        underTest.updateAdminUserPassword(newPassword, freeIpaClient);
        verify(freeIpaClient, times(1)).updatePasswordPolicy(Map.of("krbminpwdlife", 0L));
        freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, newPassword, Optional.empty());
    }

    @Test
    public void testUpdateAdminUserPasswordDontUpdatePasswordPolicy() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbminpwdlife(0);
        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);
        String newPassword = "newpassword";
        underTest.updateAdminUserPassword(newPassword, freeIpaClient);
        verify(freeIpaClient, times(0)).updatePasswordPolicy(any());
        freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, newPassword, Optional.empty());
    }
}