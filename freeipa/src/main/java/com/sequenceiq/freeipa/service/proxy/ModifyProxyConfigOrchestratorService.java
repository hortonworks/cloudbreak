package com.sequenceiq.freeipa.service.proxy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ModifyProxyConfigOrchestratorService {

    static final String MODIFY_PROXY_STATE = "modifyproxy";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigOrchestratorService.class);

    @Inject
    private StackService stackService;

    @Inject
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private FreeIpaSafeInstanceHealthDetailsService healthDetailsService;

    public void applyModifyProxyState(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<InstanceMetaData> sortedInstances = getSortedInstances(stack);

        for (InstanceMetaData instance : sortedInstances) {
            String hostName = instance.getDiscoveryFQDN();
            OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParamsForSingleTarget(stack, hostName, MODIFY_PROXY_STATE);
            LOGGER.debug("Calling applyModifyProxyState for instance {} with state params '{}'", hostName, stateParams);
            hostOrchestrator.runOrchestratorState(stateParams);
            runHealthCheck(stack, instance);
        }
    }

    /**
     * Instances have to be sorted to make sure that we are applying the state on the instances in the same order in a flow retry.
     */
    private List<InstanceMetaData> getSortedInstances(Stack stack) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .sorted(getInstanceMetaDataComparator())
                .collect(Collectors.toList());
    }

    /**
     * The order is determined by leaving the primary gateway last, other gateways are sorted by their private id.
     */
    private Comparator<InstanceMetaData> getInstanceMetaDataComparator() {
        return Comparator.comparing((InstanceMetaData i) -> InstanceMetadataType.GATEWAY_PRIMARY.equals(i.getInstanceMetadataType()))
                .thenComparing(InstanceMetaData::getPrivateId);
    }

    private void runHealthCheck(Stack stack, InstanceMetaData instance) throws CloudbreakOrchestratorException {
        NodeHealthDetails nodeHealthDetails = healthDetailsService.getInstanceHealthDetails(stack, instance);
        if (!nodeHealthDetails.getStatus().isAvailable()) {
            String issues = String.join("; ", nodeHealthDetails.getIssues());
            String message = String.format(
                    "Health check failed on instance %s after proxy configuration modification. " +
                            "Please either fix your proxy configuration settings and try the operation again, or repair the failed instance. Details: %s",
                    instance.getInstanceId(), issues);
            LOGGER.warn(message);
            throw new CloudbreakOrchestratorFailedException(message);
        }
    }
}
