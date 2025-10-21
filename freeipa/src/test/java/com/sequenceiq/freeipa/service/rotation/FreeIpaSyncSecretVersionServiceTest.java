package com.sequenceiq.freeipa.service.rotation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.vault.SyncSecretVersionService;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaSyncSecretVersionServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    @Mock
    private SyncSecretVersionService syncSecretVersionService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaSyncSecretVersionService underTest;

    @Test
    void testUpdate() {
        doNothing().when(syncSecretVersionService).updateEntityIfNeeded(any(), any(), any());
        Stack stack = new Stack();
        stack.setImage(new ImageEntity());
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(new SaltSecurityConfig());
        stack.setSecurityConfig(securityConfig);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);

        underTest.syncOutdatedSecrets(ENV_CRN);

        verify(syncSecretVersionService, times(2)).updateEntityIfNeeded(any(), any(), any());
    }
}
