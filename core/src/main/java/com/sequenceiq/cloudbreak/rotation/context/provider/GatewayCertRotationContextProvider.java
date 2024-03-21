package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.GATEWAY_CERT;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext.CustomJobRotationContextBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class GatewayCertRotationContextProvider extends AbstractKnoxCertRotationProvider {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> result = new HashMap<>();
        StackDto stack = stackDtoService.getByCrn(resourceCrn);

        GatewayView gateway = gatewayService.getByClusterId(stack.getCluster().getId())
                .map(gatewayService::putLegacyFieldsIntoVaultIfNecessary)
                .orElseThrow(() -> new CloudbreakRuntimeException(format("Cannot find Gateway in database, cluster id %s", stack.getCluster().getId())));
        GatewayView newGatewaySecrets = gatewayService.generateSignKeys(new Gateway());

        result.put(VAULT, getVaultRotationContext(stack.getResourceCrn(),
                getGatewaySignSecretMap(stack.getStack(), gateway, newGatewaySecrets)));
        result.put(CUSTOM_JOB, getCustomJobRotationContext(stack.getResourceCrn(), gateway, stack));
        result.put(CM_SERVICE_ROLE_RESTART, getCMServiceRoleRestartRotationContext(stack.getResourceCrn()));
        return result;
    }

    private Map<String, String> getGatewaySignSecretMap(StackView stack, GatewayView gateway, GatewayView newGatewaySecrets) {
        Map<String, String> signSecretMap = Map.of(gateway.getSignKeySecret().getSecret(), newGatewaySecrets.getSignKey(),
                gateway.getSignPubSecret().getSecret(), newGatewaySecrets.getSignPub(),
                gateway.getSignCertSecret().getSecret(), newGatewaySecrets.getSignCert());
        return signSecretMap;
    }

    private RotationContext getCustomJobRotationContext(String resourceCrn, GatewayView gateway, StackDto stackDto) {
        CustomJobRotationContextBuilder customJobRotationContextBuilder = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> {
                    gatewayService.setLegacyFieldsForServiceRollback(gateway);
                    clusterHostServiceRunner.updateClusterConfigs(stackDto);
                })
                .withRollbackJob(() -> {
                    gatewayService.setLegacyFieldsForServiceRollback(gateway);
                    clusterHostServiceRunner.updateClusterConfigs(stackDto);
                });
        return customJobRotationContextBuilder.build();
    }

    @Override
    public SecretType getSecret() {
        return GATEWAY_CERT;
    }

    @Override
    protected String getKnoxRole() {
        return KnoxRoles.KNOX_GATEWAY;
    }

    @Override
    protected String getSettingsStateName() {
        return "gateway";
    }
}
