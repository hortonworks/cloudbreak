package com.sequenceiq.cloudbreak.cm;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Service
@Scope("prototype")
public class ClouderaManagerClusterCommissionService implements ClusterCommissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterCommissionService.class);

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerCommissioner clouderaManagerCommissioner;

    private ApiClient client;

    public ClouderaManagerClusterCommissionService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
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
}
