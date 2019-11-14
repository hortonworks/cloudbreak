package com.sequenceiq.cloudbreak.ambari;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class AmbariClusterDecomissionService implements ClusterDecomissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterDecomissionService.class);

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private AmbariClient ambariClient;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    public AmbariClusterDecomissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void verifyNodesAreRemovable(Stack stack, Collection<InstanceMetaData> removableInstances) {
        ambariDecommissioner.verifyNodesAreRemovable(stack, removableInstances, ambariClient);
    }

    @Override
    public Set<InstanceMetaData> collectDownscaleCandidates(@Nonnull HostGroup hostGroup, Integer scalingAdjustment, int defaultRootVolumeSize,
            Set<InstanceMetaData> instanceMetaDatasInStack) throws CloudbreakException {
        return ambariDecommissioner.collectDownscaleCandidates(ambariClient, stack, hostGroup, scalingAdjustment);
    }

    @Override
    public Map<String, InstanceMetaData> collectHostsToRemove(@Nonnull HostGroup hostGroup, Set<String> hostNames) {
        return ambariDecommissioner.collectHostsToRemove(stack, hostGroup.getName(), hostNames, ambariClient);
    }

    @Override
    public Set<String> decommissionClusterNodes(Map<String, InstanceMetaData> hostsToRemove) {
        try {
            return ambariDecommissioner.decommissionAmbariNodes(stack, hostsToRemove, ambariClient);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeManagementServices() {
        // Do nothing
    }

    @Override
    public void deleteHostFromCluster(InstanceMetaData data) {
        try {
            ambariDecommissioner.deleteHostFromAmbariIfInUnknownState(data, ambariClient);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void restartStaleServices() throws CloudbreakException {
    }

    @Override
    public Map<String, Map<String, String>> getStatusOfComponentsForHost(String host) {
        return ambariDecommissioner.getStatusOfComponentsForHost(ambariClient, host);
    }
}
