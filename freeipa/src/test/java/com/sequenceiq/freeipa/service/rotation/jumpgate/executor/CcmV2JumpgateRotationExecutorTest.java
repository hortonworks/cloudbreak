package com.sequenceiq.freeipa.service.rotation.jumpgate.executor;

import static com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2RetryingClient;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.userdata.CcmUserDataService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class CcmV2JumpgateRotationExecutorTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    private static final String AGENT_CRN = "agentCrn";

    private static final String NEW_ACCESS_KEY_ID = "newAccessKeyId";

    private static final String OLD_ACCESS_KEY_ID = "oldAccessKeyId";

    private static final String NEW_ENCIPHERED_ACCESS_KEY = "newEncipheredAccessKey";

    private static final String NEW_INITIALISATION_VECTOR = "newInitialisationVector";

    private static final String NEW_HMAC_KEY = "newHmacKey";

    private static final String NEW_HMAC_FOR_PRIVATE_KEY = "newHmacForPrivateKey";

    private static final String NEW_INVERTINGPROXY_CERT = "newInvertingProxyCert";

    private static final String OLD_USER_DATA = """
            export CCM_V2_AGENT_ACCESS_KEY_ID="%s"
            export CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY="oldEncipheredAccessKey"
            export CCM_V2_IV="oldInitialisationVector"
            export CCM_V2_AGENT_HMAC_KEY="oldHmacKey"
            export CCM_V2_AGENT_HMAC_FOR_PRIVATE_KEY="oldHmarForPrivateKey"
            export CCM_V2_INVERTING_PROXY_CERTIFICATE="oldInvertingProxyCert"
            """.formatted(OLD_ACCESS_KEY_ID);

    private static final String NEW_USER_DATA = """
            export CCM_V2_AGENT_ACCESS_KEY_ID="%s"
            export CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY="%s"
            export CCM_V2_IV="%s"
            export CCM_V2_AGENT_HMAC_KEY="%s"
            export CCM_V2_AGENT_HMAC_FOR_PRIVATE_KEY="%s"
            export CCM_V2_INVERTING_PROXY_CERTIFICATE="%s"
            """.formatted(NEW_ACCESS_KEY_ID, NEW_ENCIPHERED_ACCESS_KEY, NEW_INITIALISATION_VECTOR, NEW_HMAC_KEY,
            NEW_HMAC_FOR_PRIVATE_KEY, NEW_INVERTINGPROXY_CERT);

    private static final String SECRET_PATH = "path";

    @Mock
    private StackService stackService;

    @Mock
    private ImageService imageService;

    @Mock
    private CcmV2RetryingClient ccmV2Client;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private FreeIpaStackHealthDetailsService freeIpaStackHealthDetailsService;

    @Mock
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Mock
    private CcmUserDataService ccmUserDataService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CcmV2JumpgateRotationExecutor underTest;

    @BeforeEach
    void setup() {
        lenient().when(entitlementService.isJumpgateNewRootCertEnabled(any())).thenReturn(Boolean.TRUE);
        lenient().when(entitlementService.isJumpgateRootCertAutoRotationEnabled(any())).thenReturn(Boolean.TRUE);
    }

    @Test
    void rotateWhenStackCcmParametersIsEmpty() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        when(ccmUserDataService.getHmacKeyOpt(eq(stack))).thenReturn(Optional.of(NEW_HMAC_KEY));
        InvertingProxyAgent invertingProxyAgent = InvertingProxyAgent.newBuilder()
                .setAccessKeyId(NEW_ACCESS_KEY_ID)
                .setEncipheredAccessKey(NEW_ENCIPHERED_ACCESS_KEY)
                .setInitialisationVector(NEW_INITIALISATION_VECTOR)
                .setHmacForPrivateKey(NEW_HMAC_FOR_PRIVATE_KEY)
                .build();
        InvertingProxy invertingProxy = InvertingProxy.newBuilder()
                .setCertificate(NEW_INVERTINGPROXY_CERT)
                .build();
        when(ccmV2Client.createAgentAccessKeyPair(anyString(), eq(AGENT_CRN), eq(Optional.of(NEW_HMAC_KEY)))).thenReturn(invertingProxyAgent);
        when(ccmV2Client.awaitReadyInvertingProxyForAccount(any())).thenReturn(invertingProxy);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(OLD_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        underTest.rotate(new RotationContext(ENVIRONMENT_CRN));
        verify(stackService, times(1)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        verify(ccmV2Client, times(1)).createAgentAccessKeyPair(anyString(), eq(AGENT_CRN), eq(Optional.of(NEW_HMAC_KEY)));
        verify(ccmV2Client, times(1)).awaitReadyInvertingProxyForAccount(anyString());
        verify(imageService, times(1)).getByStack(eq(stack));
        verify(ccmUserDataService, times(1)).saveOrUpdateStackCcmParameters(eq(stack), eq(invertingProxyAgent), eq(NEW_USER_DATA),
                eq(Optional.of(NEW_HMAC_KEY)), eq(Optional.of(NEW_INVERTINGPROXY_CERT)));
        verify(uncachedSecretServiceForRotation, times(1)).putRotation(any(), eq(NEW_USER_DATA));
    }

    @Test
    void rotateWhenStackCcmParametersIsNotEmpty() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        stack.setCcmParameters(new CcmConnectivityParameters(new DefaultCcmV2JumpgateParameters("invertingProxyHost", "invertingProxyCertificate",
                AGENT_CRN, "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate", ENVIRONMENT_CRN, "agentMachineUserAccessKey",
                "agentMachineUserEncipheredAccessKey", "hmacKey", "initialisationVector", "hmacForPrivateKey")));
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        when(ccmUserDataService.getHmacKeyOpt(eq(stack))).thenReturn(Optional.of(NEW_HMAC_KEY));
        InvertingProxyAgent invertingProxyAgent = InvertingProxyAgent.newBuilder()
                .setAccessKeyId(NEW_ACCESS_KEY_ID)
                .setEncipheredAccessKey(NEW_ENCIPHERED_ACCESS_KEY)
                .setInitialisationVector(NEW_INITIALISATION_VECTOR)
                .setHmacForPrivateKey(NEW_HMAC_FOR_PRIVATE_KEY)
                .build();
        InvertingProxy invertingProxy = InvertingProxy.newBuilder()
                .setCertificate(NEW_INVERTINGPROXY_CERT)
                .build();
        when(ccmV2Client.createAgentAccessKeyPair(anyString(), eq(AGENT_CRN), eq(Optional.of(NEW_HMAC_KEY)))).thenReturn(invertingProxyAgent);
        when(ccmV2Client.awaitReadyInvertingProxyForAccount(any())).thenReturn(invertingProxy);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(OLD_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        underTest.rotate(new RotationContext(ENVIRONMENT_CRN));
        verify(stackService, times(1)).getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString());
        verify(ccmV2Client, times(1)).createAgentAccessKeyPair(anyString(), eq(AGENT_CRN), eq(Optional.of(NEW_HMAC_KEY)));
        verify(ccmV2Client, times(1)).awaitReadyInvertingProxyForAccount(anyString());
        verify(imageService, times(1)).getByStack(eq(stack));
        verify(ccmUserDataService, times(1)).saveOrUpdateStackCcmParameters(eq(stack), eq(invertingProxyAgent), eq(NEW_USER_DATA),
                eq(Optional.of(NEW_HMAC_KEY)), eq(Optional.of(NEW_INVERTINGPROXY_CERT)));
        verify(uncachedSecretServiceForRotation, times(1)).putRotation(any(), eq(NEW_USER_DATA));
    }

    @Test
    void rollbackWhenUserDataSecretIsInRotation() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(NEW_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(NEW_USER_DATA, OLD_USER_DATA));
        underTest.rollback(new RotationContext(ENVIRONMENT_CRN));
        verify(ccmV2Client, times(1)).deactivateAgentAccessKeyPair(anyString(), eq(NEW_ACCESS_KEY_ID));
        verify(uncachedSecretServiceForRotation, times(1)).update(any(), eq(OLD_USER_DATA));
    }

    @Test
    void rollbackWhenUserDataSecretIsNotInRotation() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(NEW_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(OLD_USER_DATA, null));
        underTest.rollback(new RotationContext(ENVIRONMENT_CRN));
        verify(ccmV2Client, never()).deactivateAgentAccessKeyPair(anyString(), anyString());
        verify(uncachedSecretServiceForRotation, never()).update(any(), anyString());
    }

    @Test
    void finalizeRotationWhenUserDataSecretIsInRotation() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(NEW_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(NEW_USER_DATA, OLD_USER_DATA));
        underTest.finalizeRotation(new RotationContext(ENVIRONMENT_CRN));
        verify(ccmV2Client, times(1)).deactivateAgentAccessKeyPair(anyString(), eq(OLD_ACCESS_KEY_ID));
        verify(uncachedSecretServiceForRotation, times(1)).update(any(), eq(NEW_USER_DATA));
    }

    @Test
    void finalizeRotationWhenUserDataSecretIsNotInRotation() throws Exception {
        Stack stack = new Stack();
        stack.setCcmV2AgentCrn(AGENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), any())).thenReturn(stack);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata(NEW_USER_DATA);
        when(imageService.getByStack(eq(stack))).thenReturn(imageEntity);
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(OLD_USER_DATA, null));
        underTest.finalizeRotation(new RotationContext(ENVIRONMENT_CRN));
        verify(ccmV2Client, never()).deactivateAgentAccessKeyPair(anyString(), anyString());
        verify(uncachedSecretServiceForRotation, never()).update(any(), anyString());
    }

    @Test
    void preValidateShouldThrowExceptionWhenNotCcmV2Jumpgate() {
        Stack stack = new Stack();
        stack.setTunnel(Tunnel.CCMV2);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidate(new RotationContext(ENVIRONMENT_CRN)));
        assertEquals("Tunnel type is not CCM V2 Jumpgate, rotation is not possible!", secretRotationException.getMessage());
    }

    @Test
    void preValidateShouldThrowExceptionWhenStatusNotAvailable() {
        Stack stack = new Stack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        stack.setStackStatus(new StackStatus(stack, null, DetailedStackStatus.UNREACHABLE));
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidate(new RotationContext(ENVIRONMENT_CRN)));
        assertEquals("FreeIpa is not in AVAILABLE status, rotation is not possible!", secretRotationException.getMessage());
    }

    @Test
    void preValidateShouldSucceedWhenCcmV2Jumpgate() {
        Stack stack = new Stack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        stack.setStackStatus(new StackStatus(stack, null, DetailedStackStatus.AVAILABLE));
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        assertDoesNotThrow(() -> underTest.preValidate(new RotationContext(ENVIRONMENT_CRN)));
    }

    @Test
    void postValidateShouldThrowExceptionWhenFreeIpaIsNotAvailable() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setStatus(Status.UNREACHABLE);
        when(freeIpaStackHealthDetailsService.getHealthDetails(eq(ENVIRONMENT_CRN), anyString())).thenReturn(healthDetailsFreeIpaResponse);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.postValidate(new RotationContext(ENVIRONMENT_CRN)));
        assertEquals("One or more FreeIPA instance is not available. CCM V2 Jumpgate rotation was unsuccessful.", secretRotationException.getMessage());
    }

    @Test
    void postValidateShouldSucceed() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setStatus(Status.AVAILABLE);
        when(freeIpaStackHealthDetailsService.getHealthDetails(eq(ENVIRONMENT_CRN), anyString())).thenReturn(healthDetailsFreeIpaResponse);
        assertDoesNotThrow(() -> underTest.postValidate(new RotationContext(ENVIRONMENT_CRN)));
    }
}