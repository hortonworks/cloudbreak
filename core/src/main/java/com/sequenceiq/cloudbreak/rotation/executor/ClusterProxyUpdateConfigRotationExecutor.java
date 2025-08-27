package com.sequenceiq.cloudbreak.rotation.executor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyUpdateConfigRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyUpdateConfigRotationExecutor extends AbstractRotationExecutor<ClusterProxyUpdateConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyUpdateConfigRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    protected void rotate(ClusterProxyUpdateConfigRotationContext rotationContext) {
        LOGGER.info("Update stack config in cluster proxy for secret rotation.");
        updateClusterProxyConfig(rotationContext);
    }

    @Override
    protected void rollback(ClusterProxyUpdateConfigRotationContext rotationContext) {
        LOGGER.info("Update stack config in cluster proxy for rollback of secret rotation.");
        updateClusterProxyConfig(rotationContext);
    }

    private void updateClusterProxyConfig(ClusterProxyUpdateConfigRotationContext rotationContext) {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.updateClusterConfigWithKnoxSecretLocation(stackDto.getId(), rotationContext.getKnoxSecretPath());
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
