package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.vault.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public abstract class AbstractKnoxCertRotationProvider implements RotationContextProvider {

    private static final String SERVICE_TYPE_KNOX = "KNOX";

    private static final int MAX_RETRY = 10;

    protected CMServiceRoleRestartRotationContext getCMServiceRoleRestartRotationContext(String resourceCrn) {
        return CMServiceRoleRestartRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withServiceType(SERVICE_TYPE_KNOX)
                .withRoleType(getKnoxRole())
                .build();
    }

    protected SaltStateApplyRotationContext getSaltStateApplyRotationContext(StackDto stack,
            GatewayConfigService gatewayConfigService, ExitCriteriaModel exitCriteriaModel) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(stack.getResourceCrn())
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(collectNodes(stack))
                .withExitCriteriaModel(exitCriteriaModel)
                .withMaxRetry(MAX_RETRY)
                .withMaxRetryOnError(MAX_RETRY)
                .withStates(List.of(getSettingsStateName()))
                .withRollbackStates(List.of(getSettingsStateName()))
                .build();
    }

    private Set<String> collectNodes(StackDto stack) {
        return stack.getRunningInstanceMetaDataSet().stream().map(InstanceMetadataView::getDiscoveryFQDN).collect(Collectors.toSet());
    }

    protected RotationContext getVaultRotationContext(String resourceCrn, Map<String, String> newSecrets, List<Runnable> entitySavers,
            Map<String, Consumer<String>> secretUpdaterMap) {
        return VaultRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withNewSecretMap(newSecrets)
                .withEntitySecretFieldUpdaterMap(secretUpdaterMap)
                .withEntitySaverList(entitySavers)
                .build();
    }

    protected RotationContext getSaltPillarRotationContext(String resourceCrn, SaltPillarProperties pillarProperties) {
        return new SaltPillarRotationContext(resourceCrn, crn -> Map.of(getSettingsStateName(), pillarProperties));
    }

    protected abstract String getKnoxRole();

    protected abstract String getSettingsStateName();

}