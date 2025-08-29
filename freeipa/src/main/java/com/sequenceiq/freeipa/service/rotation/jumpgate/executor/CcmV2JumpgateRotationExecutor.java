package com.sequenceiq.freeipa.service.rotation.jumpgate.executor;

import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V2_AGENT_ACCESS_KEY_ID;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_HMAC_FOR_PRIVATE_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_AGENT_HMAC_KEY;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_INVERTING_PROXY_CERTIFICATE;
import static com.sequenceiq.freeipa.service.rotation.jumpgate.executor.CcmV2JumpgateUserDataEnvironmentNameConstants.CCM_V_2_IV;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.CcmV2RetryingClient;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.userdata.CcmUserDataService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class CcmV2JumpgateRotationExecutor extends AbstractRotationExecutor<RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmV2JumpgateRotationExecutor.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageService imageService;

    @Inject
    private CcmV2RetryingClient ccmV2Client;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private FreeIpaStackHealthDetailsService freeIpaStackHealthDetailsService;

    @Inject
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Inject
    private CcmUserDataService ccmUserDataService;

    @Inject
    private EntitlementService entitlementService;

    @Override
    protected void rotate(RotationContext rotationContext) throws Exception {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());

        Optional<String> hmacKey = ccmUserDataService.getHmacKeyOpt(stack);
        InvertingProxyAgent updatedInvertingProxyAgent = ccmV2Client.createAgentAccessKeyPair(environmentCrn.getAccountId(), stack.getCcmV2AgentCrn(), hmacKey);
        ImageEntity image = imageService.getByStack(stack);
        UserDataReplacer userDataReplacer = new UserDataReplacer(image.getGatewayUserdata())
                .replaceQuoted(CCM_V2_AGENT_ACCESS_KEY_ID, updatedInvertingProxyAgent.getAccessKeyId())
                .replaceQuoted(CCM_V2_AGENT_ENCIPHERED_ACCESS_KEY, updatedInvertingProxyAgent.getEncipheredAccessKey())
                .replaceQuoted(CCM_V_2_IV, updatedInvertingProxyAgent.getInitialisationVector())
                .replaceQuoted(CCM_V_2_AGENT_HMAC_KEY, hmacKey.orElse(EMPTY))
                .replaceQuoted(CCM_V_2_AGENT_HMAC_FOR_PRIVATE_KEY, updatedInvertingProxyAgent.getHmacForPrivateKey());
        Optional<String> newInvertingProxyCert = Optional.empty();
        if (entitlementService.isJumpgateNewRootCertEnabled(environmentCrn.getAccountId())) {
            InvertingProxy updatedInvertingProxy = ccmV2Client.awaitReadyInvertingProxyForAccount(environmentCrn.getAccountId());
            userDataReplacer.replaceQuoted(CCM_V_2_INVERTING_PROXY_CERTIFICATE, updatedInvertingProxy.getCertificate());
            newInvertingProxyCert = Optional.of(updatedInvertingProxy.getCertificate());
        }
        String modifiedUserData = userDataReplacer.getUserData();
        ccmUserDataService.saveOrUpdateStackCcmParameters(stack, updatedInvertingProxyAgent, modifiedUserData, hmacKey, newInvertingProxyCert);
        String newGwVaultSecretJson =
                uncachedSecretServiceForRotation.putRotation(image.getGatewayUserdataSecret().getSecret(), modifiedUserData);
        image.setGatewayUserdataSecret(new SecretProxy(newGwVaultSecretJson));
        imageService.save(image);
    }

    @Override
    protected void rollback(RotationContext rotationContext) throws Exception {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        ImageEntity image = imageService.getByStack(stack);
        RotationSecret gatewayUserDataSecret = uncachedSecretServiceForRotation.getRotation(image.getGatewayUserdataSecret().getSecret());
        if (gatewayUserDataSecret.isRotation()) {
            String newAccessKeyId = new UserDataReplacer(gatewayUserDataSecret.getSecret()).extractValue(CCM_V2_AGENT_ACCESS_KEY_ID);
            ccmV2Client.deactivateAgentAccessKeyPair(environmentCrn.getAccountId(), newAccessKeyId);
            String newGwVaultSecretJson =
                    uncachedSecretServiceForRotation.update(image.getGatewayUserdataSecret().getSecret(), gatewayUserDataSecret.getBackupSecret());
            image.setGatewayUserdataSecret(new SecretProxy(newGwVaultSecretJson));
            imageService.save(image);
        } else {
            LOGGER.warn("Gateway user data is not in rotation state in Vault, rollback is not possible, return without errors.");
        }
    }

    @Override
    protected void finalizeRotation(RotationContext rotationContext) throws Exception {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        ImageEntity image = imageService.getByStack(stack);
        RotationSecret gatewayUserDataSecret = uncachedSecretServiceForRotation.getRotation(image.getGatewayUserdataSecret().getSecret());
        if (gatewayUserDataSecret.isRotation()) {
            String originalAccessKeyId = new UserDataReplacer(gatewayUserDataSecret.getBackupSecret()).extractValue(CCM_V2_AGENT_ACCESS_KEY_ID);
            ccmV2Client.deactivateAgentAccessKeyPair(environmentCrn.getAccountId(), originalAccessKeyId);
            String newGwVaultSecretJson =
                    uncachedSecretServiceForRotation.update(image.getGatewayUserdataSecret().getSecret(), gatewayUserDataSecret.getSecret());
            image.setGatewayUserdataSecret(new SecretProxy(newGwVaultSecretJson));
            imageService.save(image);
        } else {
            LOGGER.warn("Gateway user data is not in rotation state in Vault, finalize is not possible, return without errors.");
        }
    }

    @Override
    protected void preValidate(RotationContext rotationContext) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        if (!stack.getTunnel().useCcmV2Jumpgate()) {
            throw new SecretRotationException("Tunnel type is not CCM V2 Jumpgate, rotation is not possible!");
        }
        if (!stack.isAvailable()) {
            throw new SecretRotationException("FreeIpa is not in AVAILABLE status, rotation is not possible!");
        }
    }

    @Override
    protected void postValidate(RotationContext rotationContext) throws SaltPingFailedException {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        HealthDetailsFreeIpaResponse healthDetails = freeIpaStackHealthDetailsService.getHealthDetails(resourceCrn, environmentCrn.getAccountId());
        if (healthDetails.getStatus() != Status.AVAILABLE) {
            LOGGER.warn("FreeIPA is not AVAILABLE. Details: {}", healthDetails);
            throw new SecretRotationException("One or more FreeIPA instance is not available. CCM V2 Jumpgate rotation was unsuccessful.", null);
        }
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        freeIpaSaltPingService.saltPing(stack);
    }

    @Override
    protected Class<RotationContext> getContextClass() {
        return RotationContext.class;
    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.CCMV2_JUMPGATE;
    }
}
