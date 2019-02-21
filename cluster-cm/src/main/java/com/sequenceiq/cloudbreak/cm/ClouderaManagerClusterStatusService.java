package com.sequenceiq.cloudbreak.cm;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
@Scope("prototype")
public class ClouderaManagerClusterStatusService implements ClusterStatusService {

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerClusterStatusService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public Map<String, HostMetadataState> getHostStatuses() {
        return null;
    }

    @Override
    public Map<String, String> getHostStatusesRaw() {
        return null;
    }
}
