package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxySecretProvider;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyUpdateConfigRotationContext;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyUpdateConfigRotationExecutor extends AbstractRotationExecutor<ClusterProxyUpdateConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyUpdateConfigRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private GatewayService gatewayService;

    @Inject
    private ClusterProxySecretProvider clusterProxySecretProvider;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Override
    protected void rotate(ClusterProxyUpdateConfigRotationContext rotationContext) {
        LOGGER.info("Update stack config in cluster proxy for secret rotation.");
        Optional<Gateway> gatewayOptional = gatewayService.getById(rotationContext.getCurrentGatewayId());
        if (gatewayOptional.isPresent()) {
            uncachedSecretServiceForRotation.putRotation(
                    gatewayOptional.get().getTokenCertSecret().getSecret(),
                    rotationContext.getNewGatewaySecrets().getSignCert()
            );
            updateClusterProxyConfig(rotationContext.getResourceCrn(), gatewayOptional.get().getSignCertSecret().getSecret());
        }
    }

    @Override
    protected void rollback(ClusterProxyUpdateConfigRotationContext rotationContext) {
        LOGGER.info("Update stack config in cluster proxy for rollback of secret rotation.");
        Optional<Gateway> gatewayOptional = gatewayService.getById(rotationContext.getCurrentGatewayId());
        if (gatewayOptional.isPresent()) {
            uncachedSecretServiceForRotation.putRotation(
                    gatewayOptional.get().getTokenCertSecret().getSecret(),
                    rotationContext.getNewGatewaySecrets().getSignCert()
            );
            updateClusterProxyConfig(rotationContext.getResourceCrn(), gatewayOptional.get().getSignCertSecret().getSecret());
        }
    }

    private void updateClusterProxyConfig(String resourceCrn, String secret) {
        StackDto stackDto = stackDtoService.getByCrn(resourceCrn);
        clusterProxyService.updateClusterConfigWithKnoxSecretLocation(
                stackDto.getId(),
                clusterProxySecretProvider.generateClusterProxySecretFormat(secret)
        );
    }

    @Override
    protected void finalizeRotation(ClusterProxyUpdateConfigRotationContext rotationContext) {
    }

    @Override
    protected void preValidate(ClusterProxyUpdateConfigRotationContext rotationContext) throws Exception {
    }

    @Override
    protected void postValidate(ClusterProxyUpdateConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.CLUSTER_PROXY_UPDATE;
    }

    @Override
    protected Class<ClusterProxyUpdateConfigRotationContext> getContextClass() {
        return ClusterProxyUpdateConfigRotationContext.class;
    }
}
