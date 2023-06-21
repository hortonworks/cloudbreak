package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyRotationExecutor implements RotationExecutor<ClusterProxyRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public void rotate(ClusterProxyRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    public void rollback(ClusterProxyRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for rollback of secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    public void finalize(ClusterProxyRotationContext rotationContext) {

    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.CLUSTER_PROXY;
    }

    @Override
    public Class<ClusterProxyRotationContext> getContextClass() {
        return ClusterProxyRotationContext.class;
    }
}
