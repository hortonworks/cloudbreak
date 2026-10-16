package com.sequenceiq.cloudbreak.service.migration.kraft;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class KraftAclValidationOrchestratorService {

    private static final String CHECK_KRAFT_ACLS_STATE = "kafka/check-kraft-acls";

    // The state is polled in every 10 seconds while it is running, the script itself gives up on the ACL listing after 2 minutes
    private static final int MAX_RETRY = 60;

    private static final int MAX_RETRY_ON_ERROR = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(KraftAclValidationOrchestratorService.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackUtil stackUtil;

    public void validateKraftAcls(StackDto stack) {
        Set<String> kafkaBrokerFqdns = collectKafkaBrokerFqdnsOrFail(stack);
        Set<Node> kafkaBrokerNodes = collectReachableKafkaBrokerNodesOrFail(stack, kafkaBrokerFqdns);
        LOGGER.info("Validating the KRaft cluster ACLs on the Kafka broker nodes: {}", kafkaBrokerFqdns);
        clusterHostServiceRunner.redeployStates(stack);
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParamsForTargetNodes(stack, CHECK_KRAFT_ACLS_STATE, kafkaBrokerNodes,
                MAX_RETRY, MAX_RETRY_ON_ERROR);
        try {
            hostOrchestrator.runOrchestratorState(stateParams);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("The KRaft cluster ACL validation failed on the Kafka broker nodes.", e);
            throw new CloudbreakServiceException(String.format("The KRaft cluster ACL validation failed on the Kafka broker nodes. %s",
                    getFailureReason(e)), e);
        }
        LOGGER.info("The KRaft cluster ACLs are present, the Zookeeper to KRaft migration can continue.");
    }

    private Set<String> collectKafkaBrokerFqdnsOrFail(StackDto stack) {
        Set<String> kafkaBrokerFqdns = componentLocatorService.getComponentLocationEvenIfStopped(stack,
                        cmTemplateProcessorFactory.get(stack.getBlueprintJsonText()), List.of(KafkaRoles.KAFKA_BROKER))
                .values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        // StackUtil would collect every node of the cluster for an empty host name set, so it must not be called with it
        if (kafkaBrokerFqdns.isEmpty()) {
            throw new CloudbreakServiceException("Cannot validate the KRaft cluster ACLs because the cluster has no Kafka broker node.");
        }
        return kafkaBrokerFqdns;
    }

    private Set<Node> collectReachableKafkaBrokerNodesOrFail(StackDto stack, Set<String> kafkaBrokerFqdns) {
        NodeReachabilityResult nodeReachabilityResult = stackUtil.collectReachableAndUnreachableNodes(stack, kafkaBrokerFqdns);
        if (!nodeReachabilityResult.getUnreachableNodes().isEmpty()) {
            throw new CloudbreakServiceException(String.format("Cannot start the Zookeeper to KRaft migration because the configuration management service "
                    + "is not responding on these Kafka broker nodes: %s", nodeReachabilityResult.getUnreachableHosts()));
        }
        return nodeReachabilityResult.getReachableNodes();
    }

    private String getFailureReason(CloudbreakOrchestratorFailedException e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof CloudbreakOrchestratorException orchestratorException && !orchestratorException.getNodesWithErrors().isEmpty()) {
                Multimap<String, String> nodesWithErrors = orchestratorException.getNodesWithErrors();
                return nodesWithErrors.keySet().stream()
                        .sorted()
                        .map(host -> String.format("%s: %s", host, String.join(" | ", nodesWithErrors.get(host))))
                        .collect(Collectors.joining("; "));
            }
            current = current.getCause();
        }
        return e.getMessage();
    }
}
