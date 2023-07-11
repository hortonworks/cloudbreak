package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyRotationExecutor extends AbstractRotationExecutor<ClusterProxyRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    protected void rotate(ClusterProxyRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    protected void rollback(ClusterProxyRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for rollback of secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    protected void finalize(ClusterProxyRotationContext rotationContext) {
    }

    @Override
    protected void preValidate(ClusterProxyRotationContext rotationContext) throws Exception {
        // if CP reregistration needed, then it means there is some communication with some component (CM, IPA, etc)
        // prevalidation of the given component should validate the CP connectivity itself too, thus we can skip this
    }

    @Override
    protected void postValidate(ClusterProxyRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.CLUSTER_PROXY;
    }

    @Override
    protected Class<ClusterProxyRotationContext> getContextClass() {
        return ClusterProxyRotationContext.class;
    }
}
