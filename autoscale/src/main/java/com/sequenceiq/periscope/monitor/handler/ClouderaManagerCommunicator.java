package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiConfig;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;

@Service
public class ClouderaManagerCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommunicator.class);

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private SecretService secretService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RequestLogging requestLogging;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    public Map<String, ApiConfig> getRoleConfigPropertiesFromCM(Cluster cluster, String serviceName,
            String roleGroupRef, Set roleConfigPropertyNames) {
        LOGGER.debug("Retrieving roleConfigProperties for cluster '{}', service '{}', roleGroupRef '{}'",
                cluster.getStackCrn(), serviceName, roleGroupRef);

        HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster.getStackCrn(),
                cluster.getClusterManager().getHost(), cluster.getTunnel());
        ClusterManager cm = cluster.getClusterManager();
        String user = secretService.get(cm.getUser());
        String pass = secretService.get(cm.getPass());

        Map<String, ApiConfig> roleConfigProperties = requestLogging.logResponseTime(() -> {
            try {
                ApiClient client = clouderaManagerApiClientProvider.getClient(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
                RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = new RoleConfigGroupsResourceApi(client);

                return roleConfigGroupsResourceApi
                        .readConfig(cluster.getStackName(), roleGroupRef, serviceName, DataView.FULL.name())
                        .getItems().stream()
                        .filter(apiConfig -> roleConfigPropertyNames.contains(apiConfig.getName()))
                        .collect(Collectors.toMap(ApiConfig::getName, apiConfig -> apiConfig));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, String.format("getRoleConfigPropertiesFromCM for cluster crn '%s'", cluster.getStackCrn()));

        LOGGER.debug("Retrieved roleConfigPropertyValues for cluster '{}', service '{}', roleGroupRef '{}', roleConfigProperties '{}",
                cluster.getStackCrn(), serviceName, roleGroupRef,
                roleConfigProperties.values().stream().map(apiConfig -> String.format("ApiConfig Name '%s, Value '%s', Default '%s",
                        apiConfig.getName(), apiConfig.getValue(), apiConfig.getDefault())).collect(Collectors.toSet()));
        return roleConfigProperties;
    }
}
