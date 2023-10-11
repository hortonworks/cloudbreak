package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyReRegisterRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class ClusterProxyReRegisterRotationExecutor extends AbstractRotationExecutor<ClusterProxyReRegisterRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyReRegisterRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    protected void rotate(ClusterProxyReRegisterRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    protected void rollback(ClusterProxyReRegisterRotationContext rotationContext) {
        LOGGER.info("Reregistring stack in cluster proxy for rollback of secret rotation.");
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        clusterProxyService.reRegisterCluster(stackDto.getId());
    }

    @Override
    protected void finalize(ClusterProxyReRegisterRotationContext rotationContext) {
    }

    @Override
    protected void preValidate(ClusterProxyReRegisterRotationContext rotationContext) throws Exception {
        // if CP reregistration needed, then it means there is some communication with some component (CM, IPA, etc)
        // prevalidation of the given component should validate the CP connectivity itself too, thus we can skip this
    }

    @Override
    protected void postValidate(ClusterProxyReRegisterRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.CLUSTER_PROXY_REREGISTER;
    }

    @Override
    protected Class<ClusterProxyReRegisterRotationContext> getContextClass() {
        return ClusterProxyReRegisterRotationContext.class;
    }
}
