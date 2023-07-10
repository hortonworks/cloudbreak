package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMServiceConfigRotationExecutor extends AbstractRotationExecutor<CMServiceConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMServiceConfigRotationExecutor.class);

    @Inject
    private SecretService secretService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Override
    public void rotate(CMServiceConfigRotationContext rotationContext) throws Exception {
        updateCMConfigByContext(rotationContext);
    }

    @Override
    public void rollback(CMServiceConfigRotationContext rotationContext) throws Exception {
        updateCMConfigByContext(rotationContext);
    }

    @Override
    public void finalize(CMServiceConfigRotationContext rotationContext) {

    }

    private void updateCMConfigByContext(CMServiceConfigRotationContext rotationContext) throws Exception {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        clusterApiConnectors.getConnector(stack).clusterModificationService().updateConfig(rotationContext.getCmServiceConfigTable());
    }

    @Override
    public void preValidate(CMServiceConfigRotationContext rotationContext) throws Exception {
        LOGGER.trace("CM Service config rotation will be validated during rotation to reduce CM API calls.");
    }

    @Override
    public void postValidate(CMServiceConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CM_SERVICE;
    }

    @Override
    public Class<CMServiceConfigRotationContext> getContextClass() {
        return CMServiceConfigRotationContext.class;
    }
}
