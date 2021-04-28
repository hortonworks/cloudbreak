package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.service.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.service.PollingResult.FAILURE;
import static com.sequenceiq.cloudbreak.service.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE;

import static java.util.Collections.singletonMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.model.HostStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;

@Component
class NifiDecommissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiDecommissionService.class);

    private static final String NIFI_MASTER = "NIFI_MASTER";

    private static final List<String> NIFI_COMPONENTS = List.of("NIFI_CA", NIFI_MASTER, "NIFI_REGISTRY_MASTER");

    private static final String NIFI = "NIFI";

    @Inject
    private AmbariOperationService ambariOperationService;

    void retireNifiNodes(Stack stack, AmbariClient ambariClient, List<String> hostList, Map<String, HostStatus> hostStatusMap, boolean nifiPresent) {
        if (nifiPresent) {
            List<String> hostsRunService = getHosts(hostList, hostStatusMap);
            hostsRunService.forEach(nifiNode -> retireNode(stack, ambariClient, nifiNode));
        } else {
            LOGGER.debug("Skipping retire command because Nifi components are not found.");
        }
    }

    private List<String> getHosts(List<String> hostList, Map<String, HostStatus> hostStatusMap) {
        return hostList.stream()
                .filter(hn -> hostStatusMap.get(hn).getHostComponentsStatuses().containsKey(NIFI_MASTER))
                .collect(Collectors.toList());
    }

    private void retireNode(Stack stack, AmbariClient ambariClient, String nifiNode) {
        LOGGER.info("Retiring NiFi node {}", nifiNode);
        Pair<PollingResult, Exception> retireResult = Pair.of(SUCCESS, null);
        int requestId = 0;
        try {
            requestId = ambariClient.retire(List.of(nifiNode), NIFI, NIFI_MASTER);
        } catch (Exception e) {
            LOGGER.warn("Retire command failed but we will continue the Nifi node decommission. May the current HDF version doesn't support this command.", e);
            retireResult = Pair.of(EXIT, null);
        }
        if (!isExited(retireResult.getKey())) {
            try {
                retireResult = ambariOperationService.waitForOperations(stack, ambariClient,
                        singletonMap("Retiring NiFi node", requestId), RETIRE_NIFI_NODE_AMBARI_PROGRESS_STATE);
            } catch (Exception e) {
                LOGGER.warn("Retire command failed during polling.", e);
                retireResult = Pair.of(FAILURE, e);
            }
            handlePollingResult(nifiNode, retireResult);
        }
    }

    private void handlePollingResult(String nifiNode, Pair<PollingResult, Exception> retireResult) {
        if (isSuccess(retireResult.getKey())) {
            LOGGER.info("Retirement of NiFi node {} was successful.", nifiNode);
        } else {
            Exception exception = retireResult.getValue();
            LOGGER.error("Retirement of NiFi node {} failed: {}", nifiNode, exception.getMessage());
            throw new DecommissionException(exception);
        }
    }

    void setAutoRestartForNifi(AmbariClient ambariClient, boolean nifiPresent, boolean enabled) {
        if (nifiPresent) {
            try {
                LOGGER.info("Set auto-restart to {} for the following components: {}", enabled, NIFI_COMPONENTS);
                ambariClient.setAutoRestart(NIFI_COMPONENTS, enabled);
            } catch (Exception e) {
                LOGGER.error("Failed to modify auto-restart settings.", e);
                throw new DecommissionException(e);
            }
        } else {
            LOGGER.debug("Nifi auto-restart settings are not changed because Nifi components are not found.");
        }
    }

    boolean isNifiPresentInTheCluster(Map<String, HostStatus> hostStatusMap) {
        return hostStatusMap.entrySet().stream()
                .anyMatch(entry -> entry.getValue().getHostComponentsStatuses().keySet().stream()
                        .anyMatch(componentName -> componentName.contains(NIFI)));
    }
}
