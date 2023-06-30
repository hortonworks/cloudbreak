package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;

@ExtendWith(MockitoExtension.class)
class UserKeyPairSaltStateRunRotationContextGeneratorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String PUBLICKEY_OLD = "publickey1";

    private static final String PUBLICKEY_NEW = "publickey2";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter cloudCredentialConverter;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private UserKeyPairSaltStateRunRotationContextGenerator undertest;

    @Mock
    private StackDto stackDto;

    @Mock
    private DetailedEnvironmentResponse detailedEnvironmentResponse;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private EnvironmentAuthenticationResponse environmentAuthenticationResponse;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Test
    void testGenerateContext() {
        when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        when(stackDto.getStackAuthentication()).thenReturn(stackAuthentication);
        when(detailedEnvironmentResponse.getAuthentication()).thenReturn(environmentAuthenticationResponse);
        when(stackAuthentication.getPublicKeyId()).thenReturn("pk1");
        when(environmentAuthenticationResponse.getPublicKeyId()).thenReturn("pk2");
        when(environmentAuthenticationResponse.getPublicKey()).thenReturn(PUBLICKEY_NEW);
        when(stackAuthentication.getPublicKey()).thenReturn(PUBLICKEY_OLD);

        SaltRunOrchestratorStateRotationContext context = undertest.generate(true, RESOURCE_CRN,
                stackDto, detailedEnvironmentResponse);

        assertEquals(RESOURCE_CRN, context.getResourceCrn());
        assertEquals(1, context.getStates().size());
        assertEquals(PUBLICKEY_NEW, ((Map<String, String>) context.getRotateParams().get("userssh")).get("publickey"));
        assertEquals(PUBLICKEY_OLD, ((Map<String, String>) context.getRollbackParams().get().get("userssh")).get("publickey"));
    }

    @Test
    void testGetContextNoChange() {
        SaltRunOrchestratorStateRotationContext context = undertest.generate(false, RESOURCE_CRN,
                stackDto, detailedEnvironmentResponse);

        assertEquals(RESOURCE_CRN, context.getResourceCrn());
        assertFalse(context.stateRunNeeded());
        assertNull(context.getRotateParams());
    }

}