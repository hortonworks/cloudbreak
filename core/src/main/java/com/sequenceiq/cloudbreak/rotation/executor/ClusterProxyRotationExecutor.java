package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyRotationExecutor implements RotationExecutor<ClusterProxyRotationContext> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public void rotate(ClusterProxyRotationContext rotationContext) {
        try {
            StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
            clusterProxyService.reRegisterCluster(stackDto.getId());
        } catch (Exception e) {
            throw new SecretRotationException("Reregistration in cluster proxy failed during rotation.", e, getType());
        }
    }

    @Override
    public void rollback(ClusterProxyRotationContext rotationContext) {
        try {
            StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
            clusterProxyService.reRegisterCluster(stackDto.getId());
        } catch (Exception e) {
            throw new SecretRotationException("Reregistration in cluster proxy failed during rollback.", e, getType());
        }
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
