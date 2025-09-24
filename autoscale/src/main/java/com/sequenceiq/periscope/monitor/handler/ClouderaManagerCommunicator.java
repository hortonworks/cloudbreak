package com.sequenceiq.periscope.monitor.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;

@Service
public class ClouderaManagerCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommunicator.class);

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private SecretService secretService;

    @Inject
    private RequestLogging requestLogging;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    private ApiClient createApiClient(Cluster cluster) {
        HttpClientConfig clientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster);
        ClusterManager cm = cluster.getClusterManager();
        String user = secretService.get(cm.getUser());
        String pass = secretService.get(cm.getPass());

        try {
            return clouderaManagerApiClientProvider.getV31Client(Integer.valueOf(cm.getPort()), user, pass, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.error("Error when trying to initialize CM API client for cluster: {}", cluster.getStackCrn(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isClusterManagerRunning(Cluster cluster) {
        LOGGER.info("Checking if cluster manager is running for cluster: {}", cluster.getStackCrn());
        try {
            ApiClient client = createApiClient(cluster);
            clouderaManagerApiFactory.getClouderaManagerResourceApi(client).getVersion();
            return true;
        } catch (ApiException e) {
            LOGGER.error("Error when trying to determine CM status for cluster: {}", cluster.getStackCrn(), e);
            return false;
        }
    }

    public Map<String, String> readServicesHealth(Cluster cluster) {
        ApiClient client = createApiClient(cluster);
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        List<ApiService> apiServiceList = null;
        try {
            apiServiceList = servicesResourceApi.readServices(cluster.getStackName(), DataView.FULL.name()).getItems();
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> componentWithHealthCheck = new HashMap<>();
        for (ApiService apiService : apiServiceList) {
            for (ApiHealthCheck apiHealthCheck : apiService.getHealthChecks()) {
                componentWithHealthCheck.put(apiHealthCheck.getName(), apiHealthCheck.getSummary().toString());
            }
        }
        return componentWithHealthCheck;
    }

    public Map<String, ApiConfig> getRoleConfigPropertiesFromCM(Cluster cluster, String serviceName,
        String roleGroupRef, Set roleConfigPropertyNames) {
        LOGGER.debug("Retrieving roleConfigProperties for cluster '{}', service '{}', roleGroupRef '{}'",
                cluster.getStackCrn(), serviceName, roleGroupRef);

        Map<String, ApiConfig> roleConfigProperties = requestLogging.logResponseTime(() -> {
            try {
                ApiClient client = createApiClient(cluster);
                RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);

                return roleConfigGroupsResourceApi
                        .readConfig(cluster.getStackName(), roleGroupRef, serviceName, DataView.FULL.name())
                        .getItems().stream()
                        .filter(apiConfig -> roleConfigPropertyNames.contains(apiConfig.getName()))
                        .collect(Collectors.toMap(ApiConfig::getName, apiConfig -> apiConfig));
            } catch (Exception ex) {
                throw new RuntimeException(String.format("Error retrieving roleConfigProperties for cluster '%s', service '%s', roleGroupRef '%s'",
                        cluster.getStackCrn(), serviceName, roleGroupRef), ex);
            }
        }, String.format("getRoleConfigPropertiesFromCM for cluster crn '%s'", cluster.getStackCrn()));

        LOGGER.debug("Retrieved roleConfigPropertyValues for cluster '{}', service '{}', roleGroupRef '{}', roleConfigProperties '{}",
                cluster.getStackCrn(), serviceName, roleGroupRef,
                roleConfigProperties.values().stream().map(apiConfig -> String.format("ApiConfig Name '%s, Value '%s', Default '%s",
                        apiConfig.getName(), apiConfig.getValue(), apiConfig.getDefault())).collect(Collectors.toSet()));
        return roleConfigProperties;
    }
}
