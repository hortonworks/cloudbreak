package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.StackAuthenticationRepository;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

@ExtendWith(MockitoExtension.class)
class UserKeyPairRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private StackDtoService stackService;

    @Mock
    private StackDto stackDto;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

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
    private UserKeyPairRotationContextProvider undertest;

    @Test
    void testGetContext() {
        try (MockedStatic<ThreadBasedUserCrnProvider> threadBasedUserCrnProvider = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            threadBasedUserCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(anyString(), (Supplier<DetailedEnvironmentResponse>) any()))
                    .thenReturn(detailedEnvironmentResponse);
            when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
            when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(RESOURCE_CRN);
            when(stackService.getByCrn(any())).thenReturn(stackDto);
            when(stackDto.getStackAuthentication()).thenReturn(stackAuthentication);
            when(detailedEnvironmentResponse.getAuthentication()).thenReturn(environmentAuthenticationResponse);
            when(stackAuthentication.getPublicKeyId()).thenReturn("pk1");
            when(environmentAuthenticationResponse.getPublicKeyId()).thenReturn("pk2");

            Map<SecretRotationStep, RotationContext> contexts = undertest.getContexts(RESOURCE_CRN);

            assertEquals(2, contexts.size());
            assertTrue(CloudbreakSecretType.USER_KEYPAIR.getSteps().stream().allMatch(contexts::containsKey));
        }
    }

}