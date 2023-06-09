package com.sequenceiq.cloudbreak.orchestrator.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.SERVICE_CONFIG;

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
import com.sequenceiq.cloudbreak.rotation.secret.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class ServiceConfigRotationExecutor extends AbstractRotationExecutor<ServiceConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceConfigRotationExecutor.class);

    private static final BaseEncoding BASE64 = BaseEncoding.base64();

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void rotate(ServiceConfigRotationContext rotationContext) {
        ServiceUpdateConfiguration serviceUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(serviceUpdateConfiguration);
        uploadFile(serviceUpdateConfiguration, gatewayConfig, serviceUpdateConfiguration.newConfig());
        restartSaltBootService(serviceUpdateConfiguration, gatewayConfig);
    }

    @Override
    public void rollback(ServiceConfigRotationContext rotationContext) {
        ServiceUpdateConfiguration serviceUpdateConfiguration = rotationContext.getServiceUpdateConfiguration();
        GatewayConfig gatewayConfig = getUsableGatewayConfig(serviceUpdateConfiguration);
        uploadFile(serviceUpdateConfiguration, gatewayConfig, serviceUpdateConfiguration.oldConfig());
        restartSaltBootService(serviceUpdateConfiguration, gatewayConfig);
    }

    @Override
    public void finalize(ServiceConfigRotationContext rotationContext) {

    }

    @Override
    public void preValidate(ServiceConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    public void postValidate(ServiceConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return SERVICE_CONFIG;
    }

    @Override
    public Class<ServiceConfigRotationContext> getContextClass() {
        return ServiceConfigRotationContext.class;
    }

    private GatewayConfig getUsableGatewayConfig(ServiceUpdateConfiguration serviceUpdateConfiguration) {
        GatewayConfig oldPrimaryGatewayConfig = withOldSecrets(serviceUpdateConfiguration.primaryGatewayConfig(), serviceUpdateConfiguration);
        LOGGER.info("Checking if salt boot is reachable with old secrets.");
        if (isSaltBootReachableWithGatewayConfig(serviceUpdateConfiguration, oldPrimaryGatewayConfig)) {
            LOGGER.info("Using old salt boot credentials for file upload.");
            return oldPrimaryGatewayConfig;
        }
        GatewayConfig newPrimaryGatewayConfig = withNewSecrets(serviceUpdateConfiguration.primaryGatewayConfig(), serviceUpdateConfiguration);
        if (isSaltBootReachableWithGatewayConfig(serviceUpdateConfiguration, newPrimaryGatewayConfig)) {
            LOGGER.info("Using new salt boot credentials for file upload.");
            return newPrimaryGatewayConfig;
        }
        throw new SecretRotationException(String.format(
                "Salt boot is not reachable with old nor with new secrets. %s/%s service config can't be updated.",
                serviceUpdateConfiguration.configFolder(), serviceUpdateConfiguration.configFile()),
                getType());
    }

    private boolean isSaltBootReachableWithGatewayConfig(ServiceUpdateConfiguration serviceUpdateConfiguration, GatewayConfig gatewayConfig) {
        try {
            hostOrchestrator.uploadFile(
                    gatewayConfig,
                    serviceUpdateConfiguration.targetPrivateIps(),
                    serviceUpdateConfiguration.exitCriteriaModel(),
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

    private void uploadFile(ServiceUpdateConfiguration serviceUpdateConfiguration, GatewayConfig gatewayConfig, String fileContent) {
        try {
            hostOrchestrator.uploadFile(
                    gatewayConfig,
                    serviceUpdateConfiguration.targetPrivateIps(),
                    serviceUpdateConfiguration.exitCriteriaModel(),
                    serviceUpdateConfiguration.configFolder(),
                    serviceUpdateConfiguration.configFile(),
                    fileContent.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Uploaded service configuration to {}/{} on hosts {}",
                    serviceUpdateConfiguration.configFolder(), serviceUpdateConfiguration.configFile(), serviceUpdateConfiguration.targetPrivateIps());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Couldn't upload service configuration to {}/{} on hosts {}",
                    serviceUpdateConfiguration.configFolder(), serviceUpdateConfiguration.configFile(), serviceUpdateConfiguration.targetPrivateIps(),
                    e);
            throw new SecretRotationException(e, getType());
        }
    }

    private void restartSaltBootService(ServiceUpdateConfiguration serviceConfig, GatewayConfig gatewayConfig) {
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

    private GatewayConfig withOldSecrets(GatewayConfig gatewayConfig, ServiceUpdateConfiguration serviceUpdateConfiguration) {
        return changeGatewayConfig(gatewayConfig, serviceUpdateConfiguration.oldSaltBootPassword(), serviceUpdateConfiguration.oldSaltBootPrivateKey());
    }

    private GatewayConfig withNewSecrets(GatewayConfig gatewayConfig, ServiceUpdateConfiguration serviceUpdateConfiguration) {
        return changeGatewayConfig(gatewayConfig, serviceUpdateConfiguration.newSaltBootPassword(), serviceUpdateConfiguration.newSaltBootPrivateKey());
    }

    private GatewayConfig changeGatewayConfig(GatewayConfig gatewayConfig, String saltBootPassword, String saltBootPrivateKey) {
        return gatewayConfig.toBuilder()
                .withSaltBootPassword(saltBootPassword)
                .withSignatureKey(new String(BASE64.decode(saltBootPrivateKey)))
                .build();
    }
}
