package com.sequenceiq.freeipa.service.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.SALTBOOT_CONFIG;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.freeipa.service.rotation.context.saltboot.SaltBootConfigRotationContext;
import com.sequenceiq.freeipa.service.rotation.context.saltboot.SaltBootUpdateConfiguration;

@Component
public class SaltBootConfigRotationExecutor extends AbstractRotationExecutor<SaltBootConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootConfigRotationExecutor.class);

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    protected void rotate(SaltBootConfigRotationContext rotationContext) {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(saltBootUpdateConfiguration);
        uploadFile(saltBootUpdateConfiguration, gatewayConfig, saltBootUpdateConfiguration.newConfig());
        restartSaltBootService(saltBootUpdateConfiguration, gatewayConfig);
    }

    @Override
    protected void rollback(SaltBootConfigRotationContext rotationContext) {
        SaltBootUpdateConfiguration saltBootUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(saltBootUpdateConfiguration);
        uploadFile(saltBootUpdateConfiguration, gatewayConfig, saltBootUpdateConfiguration.oldConfig());
        restartSaltBootService(saltBootUpdateConfiguration, gatewayConfig);
    }

    @Override
    protected void finalize(SaltBootConfigRotationContext rotationContext) {

    }

    @Override
    protected void preValidate(SaltBootConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(SaltBootConfigRotationContext rotationContext) throws Exception {

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
                saltBootUpdateConfiguration.configFolder(), saltBootUpdateConfiguration.configFile()),
                getType());
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
            LOGGER.error("Couldn't upload service configuration to {}/{} on hosts {}",
                    saltBootUpdateConfiguration.configFolder(), saltBootUpdateConfiguration.configFile(), saltBootUpdateConfiguration.targetPrivateIps(),
                    e);
            throw new SecretRotationException(e, getType());
        }
    }

    private void restartSaltBootService(SaltBootUpdateConfiguration serviceConfig, GatewayConfig gatewayConfig) {
        if (CollectionUtils.isNotEmpty(serviceConfig.serviceRestartActions())) {
            try {
                LOGGER.info("Executing restart actions {} on hosts {}", serviceConfig.serviceRestartActions(), serviceConfig.targetFqdns());
                hostOrchestrator.executeSaltState(
                        gatewayConfig,
                        serviceConfig.targetFqdns(),
                        serviceConfig.serviceRestartActions(),
                        serviceConfig.exitCriteriaModel(),
                        Optional.of(serviceConfig.maxRetryCount()),
                        Optional.of(serviceConfig.maxRetryCount()));
            } catch (CloudbreakOrchestratorFailedException e) {
                throw new SecretRotationException(e, getType());
            }
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
                .build();
    }
}
