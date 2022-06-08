package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsCredentialProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class SetPasswordHandlerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = CrnTestUtil.getEnvironmentCrnBuilder()
            .setResource(UUID.randomUUID().toString())
            .setAccountId(ACCOUNT_ID)
            .build().toString();

    private static final String USER = "username";

    private static final String USER_CRN = "userCrn";

    private static final long UMS_WORKLOAD_CREDENTIALS_VERSION = 123L;

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

    @Mock
    private UserMetadataConverter userMetadataConverter;

    @InjectMocks
    private SetPasswordHandler underTest;

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithUpdateOptimizationIpaCredentialsStale() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, true);

        User user = getIpaUser(USER);
        when(mockFreeIpaClient.userFind(USER)).thenReturn(Optional.of(user));
        UserMetadata userMetadata = new UserMetadata(USER_CRN, UMS_WORKLOAD_CREDENTIALS_VERSION - 1);
        doReturn(Optional.of(userMetadata)).when(userMetadataConverter).toUserMetadata(argThat(matchesUser(user)));

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(eq(true), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithUpdateOptimizationIpaCredentialsUpToDate() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, true);

        User user = getIpaUser(USER);
        when(mockFreeIpaClient.userFind(USER)).thenReturn(Optional.of(user));
        UserMetadata userMetadata = new UserMetadata(USER_CRN, UMS_WORKLOAD_CREDENTIALS_VERSION);
        doReturn(Optional.of(userMetadata)).when(userMetadataConverter).toUserMetadata(argThat(matchesUser(user)));

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(0)).setWorkloadCredential(eq(true), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithUpdateOptimizationIpaCredentialsVersionUnknown() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, true);

        User user = getIpaUser(USER);
        when(mockFreeIpaClient.userFind(USER)).thenReturn(Optional.of(user));
        doReturn(Optional.empty()).when(userMetadataConverter).toUserMetadata(argThat(matchesUser(user)));

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(eq(true), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithUmsPasswordWithoutUpdateOptimization() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(true, false);

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(eq(false), any(), any());
        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithPasswordHashSupportWithoutUmsPassword() throws FreeIpaClientException, IOException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);
        setupMocksForPasswordHashSupport(false, false);

        underTest.accept(new Event<>(request));

        verify(workloadCredentialService, times(1)).setWorkloadCredential(anyBoolean(), any(), any());
        verify(mockFreeIpaClient, times(1)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithoutPasswordHashSupport() throws FreeIpaClientException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", USER, USER_CRN, "password", Optional.empty());
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
        String hashedPassword = hasUmsPassword ? "hashedPassword" : " ";
        WorkloadCredential workloadCredential = UserSyncTestUtils.createWorkloadCredential(hashedPassword, UMS_WORKLOAD_CREDENTIALS_VERSION);
        when(umsCredentialProvider.getCredentials(any())).thenReturn(workloadCredential);

        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackService.getStackById(any())).thenReturn(stack);

        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(credentialsUpdateOptimizationEnabled);
    }

    private User getIpaUser(String uid) {
        User user = new User();
        user.setUid(uid);
        return user;
    }

    private ArgumentMatcher<User> matchesUser(User user) {
        return arg -> user.getUid().equals(arg.getUid());
    }
}