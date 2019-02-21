package com.sequenceiq.cloudbreak.ambari.datalake;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.ambari.AmbariClientProvider;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.DatalakeConfigApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service(DatalakeConfigApi.CUMULUS)
@Scope("prototype")
public class CumulusDatalakeConfigService implements DatalakeConfigApi {

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    private URL ambariUrl;

    private String ambariUserName;

    private String ambariPassword;

    private Stack stack;

    private HttpClientConfig clientConfig;

    private AmbariClient ambariClient;

    public CumulusDatalakeConfigService(URL ambariUrl, String ambariUserName, String ambariPassword) {
        this.ambariUrl = ambariUrl;
        this.ambariUserName = ambariUserName;
        this.ambariPassword = ambariPassword;
    }

    public CumulusDatalakeConfigService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        if (ambariUrl != null && StringUtils.isNoneBlank(ambariUserName, ambariPassword)) {
            ambariClient = ambariClientProvider.getAmbariClient(ambariUrl, ambariUserName, ambariPassword);
        } else if (stack != null && clientConfig != null) {
            ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
        } else {
            throw new AmbariConnectionException("Could not initialize client, missing connection paramaters");
        }
    }

    @Override
    public Map<String, String> getConfigValuesByConfigIds(List<String> configIds) {
        return ambariClient.getConfigValuesByConfigIds(configIds);
    }

    @Override
    public List<String> getHostNamesByComponent(String component) {
        return ambariClient.getHostNamesByComponent(component);
    }

    @Override
    public Map<String, Map<String, String>> getServiceComponentsMap() {
        return ambariClient.getServiceComponentsMap();
    }
}
