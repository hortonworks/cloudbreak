package com.sequenceiq.cloudbreak.service.recovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class RdsRecoverySetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryService.class);

    private static final int RECOVER_OPERATION_RETRY_COUNT = 10;

    private static final String SDX_RECOVER = "postgresql.disaster_recovery.recover_semaphore";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackService stackService;

    @Inject
    private PostgresConfigService postgresConfigService;

    public void runRecoverState(Long stackId) throws Exception {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams stateParams = createRecoverStateParams(stack);
        LOGGER.debug("Running 'recover' state with params {}", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createRecoverStateParams(Stack stack) {
        Cluster cluster = stack.getCluster();
        Set<Node> nodes = stackUtil.collectReachableNodes(stack);
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(SDX_RECOVER);
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getGatewayConfig(stack, gatewayInstance, stack.getCluster().hasGateway()));
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        stateParams.setTargetHostNames(gatewayFQDN);
        stateParams.setAllNodes(nodes);
        stateParams.setExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId()));
        stateParams.setStateParams(createRecoverParams(stack));
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetry(RECOVER_OPERATION_RETRY_COUNT);
        stateParams.setStateRetryParams(retryParams);
        return stateParams;
    }

    private Map<String, Object> createRecoverParams(Stack stack) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, stack.getCluster());
        Map<String, Object> postgresqlServerProperties =
                Optional.ofNullable(servicePillar.get("postgresql-server"))
                        .map(SaltPillarProperties::getProperties)
                        .orElse(Map.of());
        Map<String, Object> postgresCommonProperties =
                Optional.ofNullable(servicePillar.get("postgres-common"))
                        .map(SaltPillarProperties::getProperties)
                        .orElse(Map.of());

        return Stream.concat(postgresqlServerProperties.entrySet().stream(), postgresCommonProperties.entrySet().stream()).
                collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value1));
    }

}
