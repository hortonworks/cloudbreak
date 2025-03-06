package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMServiceConfigRotationExecutor extends AbstractRotationExecutor<CMServiceConfigRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMServiceConfigRotationExecutor.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Override
    protected void rotate(CMServiceConfigRotationContext rotationContext) throws Exception {
        updateCMConfigByContext(rotationContext);
    }

    @Override
    protected void rollback(CMServiceConfigRotationContext rotationContext) throws Exception {
        updateCMConfigByContext(rotationContext);
    }

    @Override
    protected void finalizeRotation(CMServiceConfigRotationContext rotationContext) {

    }

    private void updateCMConfigByContext(CMServiceConfigRotationContext rotationContext) throws Exception {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        clusterApiConnectors.getConnector(stack)
                .clusterModificationService()
                .updateConfig(rotationContext.getCmServiceConfigTable(), FALLBACK_TO_ROLLCONFIG);
    }

    @Override
    protected void preValidate(CMServiceConfigRotationContext rotationContext) throws Exception {
        LOGGER.trace("CM Service config rotation will be validated during rotation to reduce CM API calls.");
    }

    @Override
    protected void postValidate(CMServiceConfigRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CM_SERVICE;
    }

    @Override
    protected Class<CMServiceConfigRotationContext> getContextClass() {
        return CMServiceConfigRotationContext.class;
    }
}
