package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsCredentialProvider;
import com.sequenceiq.freeipa.service.stack.StackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class SetPasswordHandlerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = Crn.builder(CrnResourceDescriptor.ENVIRONMENT)
            .setResource(UUID.randomUUID().toString())
            .setAccountId(ACCOUNT_ID)
            .build().toString();

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private UmsCredentialProvider umsCredentialProvider;

    @Mock
    private WorkloadCredentialService workloadCredentialService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SetPasswordHandler underTest;

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithUpdateOptimization() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, true);

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(eq(true), any(), any(), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithoutUpdateOptimization() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, false);

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(eq(false), any(), any(), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithoutUmsPassword() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(false, false);

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(anyBoolean(), any(), any(), any(), any());
        verify(mockFreeIpaClient, times(1)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithoutPasswordHashSupport() throws FreeIpaClientException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(false);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);

        underTest.accept(new Event<>(request));

        verify(mockFreeIpaClient, times(1)).userSetPasswordWithExpiration(any(), any(), any());
    }

    private FreeIpaClient newfreeIpaClient(boolean hasPasswordHashSuppport) throws FreeIpaClientException {
        FreeIpaClient mockFreeIpaClient = mock(FreeIpaClient.class);
        Config config = mock(Config.class);
        if (hasPasswordHashSuppport) {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of("cdpUserAttr"));
        } else {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of());
        }
        when(mockFreeIpaClient.getConfig()).thenReturn(config);
        return mockFreeIpaClient;
    }

    private void setupMocksForPasswordHashSupport(boolean hasUmsPassword, boolean credentialsUpdateOptimizationEnabled) {
        WorkloadCredential workloadCredential = mock(WorkloadCredential.class);
        when(workloadCredential.getHashedPassword()).thenReturn(hasUmsPassword ? "hashedPassword" : " ");
        when(umsCredentialProvider.getCredentials(any(), any())).thenReturn(workloadCredential);

        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackService.getStackById(any())).thenReturn(stack);

        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(credentialsUpdateOptimizationEnabled);
    }
}