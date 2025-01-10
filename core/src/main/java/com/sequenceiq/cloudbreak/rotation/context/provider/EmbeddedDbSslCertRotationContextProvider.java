package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class EmbeddedDbSslCertRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        StackDto stack = stackDtoService.getByCrn(resourceCrn);
        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withPreValidateJob(() -> {
                    if (!stack.getDatabase().getExternalDatabaseAvailabilityType().isEmbedded()) {
                        throw new SecretRotationException("Embedded DB SSL certificate can be rotated only for cluster with embedded DB!");
                    }
                    if (!stack.getCluster().getDbSslEnabled()) {
                        throw new SecretRotationException("SSL is not enabled for this cluster's embedded database!");
                    }
                })
                .withResourceCrn(resourceCrn)
                .build();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        SaltStateApplyRotationContext saltStateApplyRotationContext = SaltStateApplyRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withGatewayConfig(primaryGatewayConfig)
                .withTargets(Set.of(stack.getPrimaryGatewayFQDN().
                                orElseThrow(() -> new SecretRotationException("Primary gateway FQDN cannot be found!"))))
                .withExitCriteriaModel(exitCriteriaProvider.get(stack))
                .withStates(List.of("postgresql.embeddeddb-cert-rotate.rotate"))
                .withCleanupStates(List.of("postgresql.embeddeddb-cert-rotate.finalize"))
                .withRollbackStates(List.of("postgresql.embeddeddb-cert-rotate.rollback"))
                .build();
        return Map.of(CUSTOM_JOB, customJobRotationContext,
                SALT_STATE_APPLY, saltStateApplyRotationContext);
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.EMBEDDED_DB_SSL_CERT;
    }
}
