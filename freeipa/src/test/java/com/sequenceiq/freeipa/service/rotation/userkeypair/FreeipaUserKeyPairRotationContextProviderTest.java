package com.sequenceiq.freeipa.service.rotation.userkeypair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.repository.StackAuthenticationRepository;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeipaUserKeyPairRotationContextProviderTest {

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:default:environment:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Mock
    private CachedEnvironmentClientService environmentClientService;

    @Mock
    private StackAuthenticationRepository stackAuthenticationRepository;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private EnvironmentAuthenticationResponse environmentAuthenticationResponse;

    @Mock
    private UserKeyPairSaltStateRunRotationContextGenerator userKeyPairSaltStateRunRotationContextGenerator;

    @InjectMocks
    private FreeipaUserKeypairRotationContextProvider undertest;

    @Test
    void testGetContext() {
        try (MockedStatic<ThreadBasedUserCrnProvider> threadBasedUserCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class)) {
            threadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor((Supplier<DetailedEnvironmentResponse>) any()))
                    .thenReturn(detailedEnvironmentResponse);
            when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);
            when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
            when(detailedEnvironmentResponse.getAuthentication()).thenReturn(environmentAuthenticationResponse);
            when(stackAuthentication.getPublicKeyId()).thenReturn("pk1");
            when(environmentAuthenticationResponse.getPublicKeyId()).thenReturn("pk2");

            Map<SecretRotationStep, RotationContext> contexts = undertest.getContexts(RESOURCE_CRN);

            assertEquals(2, contexts.size());
            assertTrue(FreeIpaSecretType.USER_KEYPAIR.getSteps().stream().allMatch(contexts::containsKey));
        }
    }

}