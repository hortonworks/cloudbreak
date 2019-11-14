package com.sequenceiq.cloudbreak.ambari.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.ambari.AmbariClusterStatusFactory;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
@Scope("prototype")
public class AmbariClusterStatusService implements ClusterStatusService {

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private AmbariClusterStatusFactory clusterStatusFactory;

    private AmbariClient ambariClient;

    public AmbariClusterStatusService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public ClusterStatusResult getStatus(boolean blueprintPresent) {
        return clusterStatusFactory.createClusterStatus(stack, clientConfig, blueprintPresent);
    }

    @Override
    public Map<String, ClusterManagerState.ClusterManagerStatus> getHostStatuses() {
        Map<String, String> hostStatuses = ambariClient.getHostStatuses();
        Map<String, ClusterManagerState.ClusterManagerStatus> hostMetadataStateMap = new HashMap<>();
        for (Entry<String, String> entry : hostStatuses.entrySet()) {
            ClusterManagerState.ClusterManagerStatus state = ClusterManagerState.ClusterManagerStatus.HEALTHY.name().equals(entry.getValue())
                    ? ClusterManagerState.ClusterManagerStatus.HEALTHY : ClusterManagerState.ClusterManagerStatus.UNHEALTHY;
            hostMetadataStateMap.put(entry.getKey(), state);
        }
        return hostMetadataStateMap;
    }

    @Override
    public Map<String, ClusterManagerState> getExtendedHostStatuses() {
        return getHostStatuses().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), new ClusterManagerState(entry.getValue(), null)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    @Override
    public Map<String, String> getHostStatusesRaw() {
        return ambariClient.getHostStatuses();
    }
}
