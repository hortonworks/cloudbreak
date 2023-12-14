package com.sequenceiq.freeipa.service.rotation.executor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.rotation.context.SaltPillarUpdateRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class SaltPillarUpdateRotationExecutor extends AbstractRotationExecutor<SaltPillarUpdateRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPillarUpdateRotationExecutor.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Override
    public void rotate(SaltPillarUpdateRotationContext rotationContext) throws Exception {
        LOGGER.info("Update pillar for rotate phase");
        updatePillar(rotationContext);
    }

    @Override
    public void rollback(SaltPillarUpdateRotationContext rotationContext) {
        LOGGER.info("Update pillar for rollback phase");
        updatePillar(rotationContext);
    }

    private void updatePillar(SaltPillarUpdateRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        LOGGER.info("Update pillar for: {}", stack.getResourceCrn());
        stack.getPrimaryGateway()
                .ifPresent(pgwInstanceMetadata -> {
                    LOGGER.info("Preparing freeipa pillar update");
                    Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
                    Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
                    Map<String, SaltPillarProperties> servicePillar = rotationContext.getServicePillarGenerator().apply(stack);
                    LOGGER.info("Salt pillar keys: {}", servicePillar.keySet());
                    OrchestratorStateParams stateParams = new OrchestratorStateParams();
                    stateParams.setTargetHostNames(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
                    GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
                    stateParams.setPrimaryGatewayConfig(gatewayConfig);
                    try {
                        LOGGER.info("Updating freeipa pillar");
                        hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar), null, stateParams);
                    } catch (CloudbreakOrchestratorFailedException e) {
                        LOGGER.error("Can't save freeipa pillar", e);
                        throw new CloudbreakRuntimeException(e);
                    }
                });
    }

    @Override
    public void finalize(SaltPillarUpdateRotationContext rotationContext) {

    }

    @Override
    public void preValidate(SaltPillarUpdateRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        LOGGER.info("Validate for pillar update: {}", stack.getResourceCrn());
        try {
            freeIpaSaltPingService.saltPing(stack);
        } catch (SaltPingFailedException e) {
            throw new CloudbreakRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void postValidate(SaltPillarUpdateRotationContext rotationContext) {

    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.SALT_PILLAR_UPDATE;
    }

    @Override
    public Class<SaltPillarUpdateRotationContext> getContextClass() {
        return SaltPillarUpdateRotationContext.class;
    }
}
