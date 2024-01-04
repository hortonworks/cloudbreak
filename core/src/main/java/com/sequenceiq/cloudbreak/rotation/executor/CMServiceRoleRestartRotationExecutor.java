package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class CMServiceRoleRestartRotationExecutor extends AbstractRotationExecutor<CMServiceRoleRestartRotationContext> {

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackDtoService stackService;

    @Override
    public void rotate(CMServiceRoleRestartRotationContext rotationContext) throws Exception {
        restart(rotationContext);
    }

    @Override
    public void rollback(CMServiceRoleRestartRotationContext rotationContext) throws Exception {
        restart(rotationContext);
    }

    private void restart(CMServiceRoleRestartRotationContext context) {
        StackDto stack = stackService.getByCrn(context.getResourceCrn());
        clusterApiConnectors.getConnector(stack)
                .clusterModificationService()
                .restartServiceRoleByType(context.getServiceType(), context.getRoleType());
    }

    @Override
    public void finalize(CMServiceRoleRestartRotationContext rotationContext) throws Exception {

    }

    @Override
    public void preValidate(CMServiceRoleRestartRotationContext rotationContext) throws Exception {
        StackDto stack = stackService.getByCrn(rotationContext.getResourceCrn());
        boolean serviceRunning = clusterApiConnectors.getConnector(stack)
                .clusterStatusService()
                .isServiceRunningByType(stack.getName(), rotationContext.getServiceType());
        if (!serviceRunning) {
            throw new CloudbreakException(String.format("CM service type '%s' is not running or unreachable in cluster '%s'.",
                    rotationContext.getServiceType(), stack.getName()));
        }
    }

    @Override
    public void postValidate(CMServiceRoleRestartRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CM_SERVICE_ROLE_RESTART;
    }

    @Override
    public Class<CMServiceRoleRestartRotationContext> getContextClass() {
        return CMServiceRoleRestartRotationContext.class;
    }
}
