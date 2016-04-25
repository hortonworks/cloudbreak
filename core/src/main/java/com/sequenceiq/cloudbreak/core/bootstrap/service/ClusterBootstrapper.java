package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.MUNCHAUSEN;
import static com.sequenceiq.cloudbreak.service.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.service.PollingResult.TIMEOUT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CbBootResponse;
import com.sequenceiq.cloudbreak.api.model.CbBootResponses;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.BootstrapApiContext;
import com.sequenceiq.cloudbreak.core.flow.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class ClusterBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBootstrapper.class);

    private static final int POLLING_INTERVAL = 5000;

    private static final int MAX_POLLING_ATTEMPTS = 500;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private OrchestratorRepository orchestratorRepository;

    @Inject
    private PollingService<BootstrapApiContext> bootstrapApiPollingService;

    @Inject
    private BootstrapApiCheckerTask bootstrapApiCheckerTask;

    @Inject
    private PollingService<ContainerOrchestratorClusterContext> clusterAvailabilityPollingService;

    @Inject
    private ClusterAvailabilityCheckerTask clusterAvailabilityCheckerTask;

    @Inject
    private ClusterBootstrapperErrorHandler clusterBootstrapperErrorHandler;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ContainerConfigService containerConfigService;

    @SuppressWarnings("unchecked")
    public void bootstrapOnHost(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        Set<Node> nodes = new HashSet<>();
        Set<String> targets = new HashSet<>(Arrays.asList(gatewayInstance.getPrivateIp()));
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            targets.add(instanceMetaData.getPrivateIp());
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp()));
        }
        try {
            // TODO replace it with object
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("data_dir", "/etc/cloudbreak/consul");
            // TODO multiple servers
            configMap.put("servers", Arrays.asList(gatewayInstance.getPrivateIp()));
            configMap.put("targets", targets);

            String plainCreds = "cbadmin:cbadmin";
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + base64Creds);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity req = new HttpEntity<>(JsonUtil.writeValueAsString(configMap), headers);

            Set<String> missingTargets = new HashSet<>(targets);
            while (!missingTargets.isEmpty()) {
                LOGGER.info("Sending consul config save request to {}", missingTargets);
                ResponseEntity<CbBootResponses> response = restTemplate.exchange(
                        "http://" + gatewayInstance.getPublicIp() + ":8088/cbboot/consul/config/distribute", HttpMethod.POST, req, CbBootResponses.class);
                CbBootResponses responseBody = response.getBody();
                for (CbBootResponse cbBootResponse : responseBody.getResponses()) {
                    if (cbBootResponse.getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
                        LOGGER.info("Successfully distributed consul config to: " + cbBootResponse.getAddress());
                        missingTargets.remove(cbBootResponse.getAddress().split(":")[0]);
                    }
                }
                configMap.put("targets", missingTargets);
                req = new HttpEntity<>(JsonUtil.writeValueAsString(configMap), headers);
                if (!missingTargets.isEmpty()) {
                    LOGGER.info("Missing nodes to save consul config: %s", missingTargets);
                }
                Thread.sleep(5000);
            }

            Map<String, Object> consulRunMap = new HashMap<>();
            consulRunMap.put("targets", targets);
            HttpEntity req2 = new HttpEntity<>(JsonUtil.writeValueAsString(consulRunMap), headers);
            ResponseEntity<CbBootResponses> response = restTemplate.exchange(
                    "http://" + gatewayInstance.getPublicIp() + ":8088/cbboot/consul/run/distribute", HttpMethod.POST, req2, CbBootResponses.class);
            CbBootResponses responseBody = response.getBody();
            LOGGER.info("Consul run response: %s", responseBody);
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapCluster(ProvisioningContext provisioningContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper()));
        }
        try {
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(), gatewayInstance.getPublicIpWrapper(),
                    stack.getGatewayPort(), gatewayInstance.getPrivateIp());
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get("SWARM");
            PollingResult bootstrapApiPolling = bootstrapApiPollingService.pollWithTimeoutSingleFailure(
                    bootstrapApiCheckerTask,
                    new BootstrapApiContext(stack, gatewayConfig, containerOrchestrator),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(bootstrapApiPolling, "Polling of bootstrap API was cancelled.");

            List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator, gatewayInstance.getPublicIpWrapper());
            containerOrchestrator.bootstrap(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap.get(0), stack.getConsulServers(),
                    clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
            if (nodeMap.size() > 1) {
                PollingResult gatewayAvailability = clusterAvailabilityPollingService.pollWithTimeoutSingleFailure(clusterAvailabilityCheckerTask,
                        new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap.get(0)),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS);
                validatePollingResultForCancellation(gatewayAvailability, "Polling of gateway node availability was cancelled.");
                for (int i = 1; i < nodeMap.size(); i++) {
                    containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap.get(i),
                            clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
                    PollingResult agentAvailability = clusterAvailabilityPollingService.pollWithTimeoutSingleFailure(clusterAvailabilityCheckerTask,
                            new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap.get(i)),
                            POLLING_INTERVAL,
                            MAX_POLLING_ATTEMPTS);
                    validatePollingResultForCancellation(agentAvailability, "Polling of agent nodes availability was cancelled.");
                }
            }
            PollingResult allNodesAvailabilityPolling = clusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(allNodesAvailabilityPolling, "Polling of all nodes availability was cancelled.");
            // TODO: put it in orchestrator api
            Orchestrator orchestrator = new Orchestrator();
            orchestrator.setApiEndpoint(gatewayInstance.getPublicIpWrapper() + ":" + stack.getGatewayPort());
            orchestrator.setType("SWARM");
            orchestratorRepository.save(orchestrator);
            stack.setOrchestrator(orchestrator);
            stackRepository.save(stack);
            if (TIMEOUT.equals(allNodesAvailabilityPolling)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(containerOrchestrator, stack, gatewayConfig, nodes);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void bootstrapNewNodes(StackScalingContext stackScalingContext) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackScalingContext.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();

        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (stackScalingContext.getUpscaleCandidateAddresses().contains(instanceMetaData.getPrivateIp())) {
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIpWrapper()));
            }
        }
        try {
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                    gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp());
            ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get("SWARM");
            List<Set<Node>> nodeMap = prepareBootstrapSegments(nodes, containerOrchestrator, gatewayInstance.getPublicIpWrapper());
            for (int i = 0; i < nodeMap.size(); i++) {
                containerOrchestrator.bootstrapNewNodes(gatewayConfig, containerConfigService.get(stack, MUNCHAUSEN), nodeMap.get(i),
                        clusterDeletionBasedExitCriteriaModel(stack.getId(), null));
                PollingResult newNodesAvailabilityPolling = clusterAvailabilityPollingService.pollWithTimeoutSingleFailure(clusterAvailabilityCheckerTask,
                        new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodeMap.get(i)),
                        POLLING_INTERVAL,
                        MAX_POLLING_ATTEMPTS);
                validatePollingResultForCancellation(newNodesAvailabilityPolling, "Polling of new nodes availability was cancelled.");
            }
            PollingResult pollingResult = clusterAvailabilityPollingService.pollWithTimeoutSingleFailure(
                    clusterAvailabilityCheckerTask,
                    new ContainerOrchestratorClusterContext(stack, containerOrchestrator, gatewayConfig, nodes),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            validatePollingResultForCancellation(pollingResult, "Polling of new nodes availability was cancelled.");
            if (TIMEOUT.equals(pollingResult)) {
                clusterBootstrapperErrorHandler.terminateFailedNodes(containerOrchestrator, stack, gatewayConfig, nodes);
            }
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private List<Set<Node>> prepareBootstrapSegments(Set<Node> nodes, ContainerOrchestrator containerOrchestrator, String gatewayIp) {
        List<Set<Node>> result = new ArrayList<>();
        Set<Node> newNodes = new HashSet<>();
        Node gatewayNode = getGateWayNode(nodes, gatewayIp);
        if (gatewayNode != null) {
            newNodes.add(gatewayNode);
        }
        for (Node node : nodes) {
            if (!gatewayIp.equals(node.getPublicIp())) {
                newNodes.add(node);
                if (newNodes.size() >= containerOrchestrator.getMaxBootstrapNodes()) {
                    result.add(newNodes);
                    newNodes = new HashSet<>();
                }
            }
        }
        if (!newNodes.isEmpty()) {
            result.add(newNodes);
        }
        return result;
    }

    private Node getGateWayNode(Set<Node> nodes, String gatewayIp) {
        for (Node node : nodes) {
            if (gatewayIp.equals(node.getPublicIp())) {
                return node;
            }
        }
        return null;
    }

    private void validatePollingResultForCancellation(PollingResult pollingResult, String cancelledMessage) {
        if (EXIT.equals(pollingResult)) {
            throw new CancellationException(cancelledMessage);
        }
    }

}
