package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterCommissionService implements ClusterCommissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterCommissionService.class);

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerCommissioner clouderaManagerCommissioner;

    private ApiClient client;

    public ClouderaManagerClusterCommissionService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public Map<String, InstanceMetaData> collectHostsToCommission(@Nonnull HostGroup hostGroup, Set<String> hostNames) {
        return clouderaManagerCommissioner.collectHostsToCommission(stack, hostGroup, hostNames, client);
    }

    @Override
    public Set<String> recommissionClusterNodes(Map<String, InstanceMetaData> hostsToRecommission) {
        return clouderaManagerCommissioner.recommissionNodes(stack, hostsToRecommission, client);
    }

    @Override
    public void recommissionHosts(List<String> hostsToRecommission) {
        clouderaManagerCommissioner.recommissionHosts(stack, client, hostsToRecommission);
    }
}
