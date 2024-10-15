package com.sequenceiq.freeipa.service.rotation.nginxclustersslkey;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.SALT_STATE_APPLY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class NginxClusterSslCertPrivateKeyRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NginxClusterSslCertPrivateKeyRotationContextProvider.class);

    private static final String PREPARE_STATE = "nginx.rotatesslclusterkey.prepare";

    private static final String ROTATE_STATE = "nginx.rotatesslclusterkey.rotate";

    private static final String ROLLBACK_STATE = "nginx.rotatesslclusterkey.rollback";

    private static final String FINALIZE_STATE = "nginx.rotatesslclusterkey.finalize";

    private static final String CMD_RETRIEVE_OLD_CERT = "cat /etc/certs-backup/cluster.pem";

    private static final String CMD_RETRIEVE_NEW_CERT = "cat /etc/certs-new-temp/cluster.pem";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private SecretRotationSaltService secretRotationSaltService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, Crn.safeFromString(resourceCrn).getAccountId());
        Map<SecretRotationStep, RotationContext> context = new HashMap<>();
        context.put(SALT_STATE_APPLY, getSaltStateApplyRotationContext(stack));
        context.put(CUSTOM_JOB, CustomJobRotationContext.builder()
                .withRotationJob(() -> switchCertAndUpdateImd(stack, CMD_RETRIEVE_NEW_CERT, ROTATE_STATE))
                .withRollbackJob(() -> switchCertAndUpdateImd(stack, CMD_RETRIEVE_OLD_CERT, ROLLBACK_STATE))
                .build());
        return context;
    }

    private void switchCertAndUpdateImd(Stack stack, String revealCertCommand, String saltStateToApply) {
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, stack.getNotDeletedInstanceMetaDataSet());
            GatewayConfig primaryGatewayConfig = gatewayConfigs.stream().filter(GatewayConfig::isPrimary).findFirst().orElseThrow();
            Set<String> targets = gatewayConfigs.stream().map(GatewayConfig::getHostname).collect(Collectors.toSet());
            Map<String, String> certs = secretRotationSaltService.executeCommand(gatewayConfigs, targets, revealCertCommand);
            certs.forEach((key, value) -> gatewayConfigs.stream().filter(gc -> StringUtils.equals(gc.getHostname(), key))
                    .findFirst().orElseThrow().withNewServerCert(value));
            secretRotationSaltService.executeSaltState(primaryGatewayConfig, targets, List.of(saltStateToApply), exitCriteriaProvider.get(stack),
                    Optional.empty(), Optional.empty());
            certs.forEach((key, value) -> {
                String instanceId = gatewayConfigs.stream().filter(gc -> StringUtils.equals(gc.getHostname(), key)).findFirst().orElseThrow().getInstanceId();
                instanceMetaDataService.updateServerCert(BaseEncoding.base64().encode(value.getBytes()), instanceId, key);
            });
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Failed to execute salt call!", e);
            throw new SecretRotationException(e);
        }
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.NGINX_CLUSTER_SSL_CERT_PRIVATE_KEY;
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContext(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(stack.getAllInstanceMetaDataList().stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of(PREPARE_STATE))
                .withCleanupStates(List.of(FINALIZE_STATE))
                .build();
    }
}
