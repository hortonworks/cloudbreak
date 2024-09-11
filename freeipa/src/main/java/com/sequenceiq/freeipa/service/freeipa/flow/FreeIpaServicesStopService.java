package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.PrimaryGatewayFirstThenSortByFqdnComparator;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaServicesStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaServicesStopService.class);

    @Value("${freeipa.delayed.stop-start-sec}")
    private long delayInSec;

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private DelayedExecutorService delayedExecutorService;

    public void stopServices(Long stackId) throws ExecutionException, InterruptedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<InstanceMetaData> instancesToStopOrdered = stack.getNotDeletedInstanceMetaDataSet().stream()
                .sorted(new PrimaryGatewayFirstThenSortByFqdnComparator().reversed())
                .toList();
        if (!instancesToStopOrdered.isEmpty()) {
            stopServicesOnInstance(stack, instancesToStopOrdered.getFirst());
            if (instancesToStopOrdered.size() > 1) {
                for (InstanceMetaData instance : instancesToStopOrdered.subList(1, instancesToStopOrdered.size())) {
                    delayedExecutorService.runWithDelay(() -> stopServicesOnInstance(stack, instance), delayInSec, TimeUnit.SECONDS);
                }
            }
        } else {
            LOGGER.warn("There are no instances to stop for stack: {}", stack);
        }
    }

    private void stopServicesOnInstance(Stack stack, InstanceMetaData instanceMetaData) {
        try {
            LOGGER.info("Stopping instance: {}", instanceMetaData);
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            hostOrchestrator.runCommandOnHosts(List.of(gatewayConfig), Set.of(instanceMetaData.getDiscoveryFQDN()), "ipactl stop");
            LOGGER.info("Stopped instance: {}", instanceMetaData.getDiscoveryFQDN());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Failed to stop services on {}", instanceMetaData, e);
        }
    }
}
