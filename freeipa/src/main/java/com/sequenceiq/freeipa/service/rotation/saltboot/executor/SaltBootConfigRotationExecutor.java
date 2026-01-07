package com.sequenceiq.freeipa.service.rotation.saltboot.executor;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayServiceConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.rotation.saltboot.context.SaltBootConfigRotationContext;
import com.sequenceiq.freeipa.service.rotation.saltboot.contextprovider.SaltBootUpdateConfiguration;

@Component
public class SaltBootConfigRotationExecutor extends AbstractRotationExecutor<SaltBootConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootConfigRotationExecutor.class);

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Override
    protected void rotate(SaltBootConfigRotationContext rotationContext) throws CloudbreakOrchestratorFailedException {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(saltBootUpdateConfiguration);
        uploadFile(saltBootUpdateConfiguration, gatewayConfig, saltBootUpdateConfiguration.newConfig());
        restartSaltBootService(saltBootUpdateConfiguration, gatewayConfig);
    }

    @Override
    protected void rollback(SaltBootConfigRotationContext rotationContext) throws CloudbreakOrchestratorFailedException {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(saltBootUpdateConfiguration);
        uploadFile(saltBootUpdateConfiguration, gatewayConfig, saltBootUpdateConfiguration.oldConfig());
        restartSaltBootService(saltBootUpdateConfiguration, gatewayConfig);
    }

    @Override
    protected void finalizeRotation(SaltBootConfigRotationContext rotationContext) {

    }

    @Override
    protected void preValidate(SaltBootConfigRotationContext rotationContext) throws Exception {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = withOldSecrets(saltBootUpdateConfiguration.primaryGatewayConfig(), saltBootUpdateConfiguration);
        secretRotationSaltService.validateSalt(rotationContext.getServiceUpdateConfiguration().targetFqdns(), gatewayConfig);
    }

    @Override
    protected void postValidate(SaltBootConfigRotationContext rotationContext) throws Exception {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = withNewSecrets(saltBootUpdateConfiguration.primaryGatewayConfig(), saltBootUpdateConfiguration);
        secretRotationSaltService.validateSalt(rotationContext.getServiceUpdateConfiguration().targetFqdns(), gatewayConfig);
    }

    @Override
    public SecretRotationStep getType() {
        return SALTBOOT_CONFIG;
    }

    @Override
    protected Class<SaltBootConfigRotationContext> getContextClass() {
        return SaltBootConfigRotationContext.class;
    }

    private GatewayConfig getUsableGatewayConfig(SaltBootUpdateConfiguration saltBootUpdateConfiguration) {
        GatewayConfig oldPrimaryGatewayConfig = withOldSecrets(saltBootUpdateConfiguration.primaryGatewayConfig(), saltBootUpdateConfiguration);
        LOGGER.info("Checking if salt boot is reachable with old secrets.");
        if (isSaltBootReachableWithGatewayConfig(saltBootUpdateConfiguration, oldPrimaryGatewayConfig)) {
            LOGGER.info("Using old salt boot credentials for file upload.");
            return oldPrimaryGatewayConfig;
        }
        GatewayConfig newPrimaryGatewayConfig = withNewSecrets(saltBootUpdateConfiguration.primaryGatewayConfig(), saltBootUpdateConfiguration);
        if (isSaltBootReachableWithGatewayConfig(saltBootUpdateConfiguration, newPrimaryGatewayConfig)) {
            LOGGER.info("Using new salt boot credentials for file upload.");
            return newPrimaryGatewayConfig;
        }
        throw new SecretRotationException(String.format(
                "Salt boot is not reachable with old nor with new secrets. %s/%s service config can't be updated.",
                saltBootUpdateConfiguration.configFolder(), saltBootUpdateConfiguration.configFile()));
    }

    private boolean isSaltBootReachableWithGatewayConfig(SaltBootUpdateConfiguration saltBootUpdateConfiguration, GatewayConfig gatewayConfig) {
        try {
            hostOrchestrator.uploadFile(
                    gatewayConfig,
                    saltBootUpdateConfiguration.targetPrivateIps(),
                    saltBootUpdateConfiguration.exitCriteriaModel(),
                    "/tmp",
                    "saltboottest-" + System.currentTimeMillis(),
                    "test".getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Salt boot is reachable with gateway config.");
            return true;
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.info("Salt boot is not reachable with gateway config.", e);
            return false;
        }
    }

    private void uploadFile(SaltBootUpdateConfiguration saltBootUpdateConfiguration, GatewayConfig gatewayConfig, String fileContent) {
        try {
            hostOrchestrator.uploadFile(
                    gatewayConfig,
                    saltBootUpdateConfiguration.targetPrivateIps(),
                    saltBootUpdateConfiguration.exitCriteriaModel(),
                    saltBootUpdateConfiguration.configFolder(),
                    saltBootUpdateConfiguration.configFile(),
                    fileContent.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Uploaded service configuration to {}/{} on hosts {}",
                    saltBootUpdateConfiguration.configFolder(), saltBootUpdateConfiguration.configFile(), saltBootUpdateConfiguration.targetPrivateIps());
        } catch (CloudbreakOrchestratorFailedException e) {
            String message = String.format("Couldn't upload service configuration to %s/%s on hosts %s",
                    saltBootUpdateConfiguration.configFolder(), saltBootUpdateConfiguration.configFile(), saltBootUpdateConfiguration.targetPrivateIps());
            LOGGER.error(message, e);
            throw new SecretRotationException(message, e);
        }
    }

    private void restartSaltBootService(SaltBootUpdateConfiguration serviceConfig, GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        if (CollectionUtils.isNotEmpty(serviceConfig.serviceRestartActions())) {
            LOGGER.info("Executing restart actions {} on hosts {}", serviceConfig.serviceRestartActions(), serviceConfig.targetFqdns());
            hostOrchestrator.executeSaltState(
                    gatewayConfig,
                    serviceConfig.targetFqdns(),
                    serviceConfig.serviceRestartActions(),
                    serviceConfig.exitCriteriaModel(),
                    Optional.of(serviceConfig.maxRetryCount()),
                    Optional.of(serviceConfig.maxRetryCount()));
        }
    }

    private GatewayConfig withOldSecrets(GatewayConfig gatewayConfig, SaltBootUpdateConfiguration saltBootUpdateConfiguration) {
        return changeGatewayConfig(gatewayConfig, saltBootUpdateConfiguration.oldSaltBootPassword(), saltBootUpdateConfiguration.oldSaltBootPrivateKey());
    }

    private GatewayConfig withNewSecrets(GatewayConfig gatewayConfig, SaltBootUpdateConfiguration saltBootUpdateConfiguration) {
        return changeGatewayConfig(gatewayConfig, saltBootUpdateConfiguration.newSaltBootPassword(), saltBootUpdateConfiguration.newSaltBootPrivateKey());
    }

    private GatewayConfig changeGatewayConfig(GatewayConfig gatewayConfig, String saltBootPassword, String saltBootPrivateKey) {
        return gatewayConfig.toBuilder()
                .withSaltBootPassword(saltBootPassword)
                .withSignatureKey(new String(BASE64.decode(saltBootPrivateKey)))
                .withGatewayServiceConfig(GatewayServiceConfig.builder().build())
                .build();
    }
}
