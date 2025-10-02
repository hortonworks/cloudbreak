package com.sequenceiq.freeipa.service.rotation.jumpgate.contextprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.freeipa.flow.SaltConfigProvider;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.rotation.ExitCriteriaProvider;
import com.sequenceiq.freeipa.service.rotation.context.SaltPillarUpdateRotationContext;
import com.sequenceiq.freeipa.service.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class CcmV2JumpgateAgentAccessKeyRotationContextProvider implements RotationContextProvider {

    private static final String TRIGGER_CCM_ROTATION_STATE = "rotateccm";

    private static final String FINALIZE_CCM_ROTATION_STATE = "rotateccm/finalize";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private SaltConfigProvider saltConfigProvider;

    @Inject
    private ImageService imageService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        contexts.put(FreeIpaSecretRotationStep.CCMV2_JUMPGATE, new RotationContext(resourceCrn));
        contexts.put(FreeIpaSecretRotationStep.LAUNCH_TEMPLATE, new RotationContext(resourceCrn));
        contexts.put(FreeIpaSecretRotationStep.SALT_PILLAR_UPDATE, getSaltPillarUpdateRotationContext(resourceCrn));
        contexts.put(FreeIpaSecretRotationStep.SALT_STATE_APPLY, getSaltStateApplyRotationContext(resourceCrn));
        return contexts;
    }

    private SaltPillarUpdateRotationContext getSaltPillarUpdateRotationContext(String resourceCrn) {
        return SaltPillarUpdateRotationContext.builder()
                .withEnvironmentCrn(resourceCrn)
                .withServicePillarGenerator(stack -> saltConfigProvider.getCcmPillarProperties(stack))
                .build();
    }

    private SaltStateApplyRotationContext getSaltStateApplyRotationContext(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        return SaltStateApplyRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of(TRIGGER_CCM_ROTATION_STATE))
                .withCleanupStates(List.of(FINALIZE_CCM_ROTATION_STATE))
                .build();
    }

    @Override
    public SecretType getSecret() {
        return FreeIpaSecretType.CCMV2_JUMPGATE_AGENT_ACCESS_KEY;
    }

    @Override
    public Set<String> getVaultSecretsForRollback(String resourceCrn) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        ImageEntity image = imageService.getByStack(stack);
        if (image.getGatewayUserdataSecret().getSecret() != null) {
            return Set.of(image.getGatewayUserdataSecret().getSecret());
        } else {
            return Set.of();
        }
    }
}
