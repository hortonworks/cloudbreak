package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmOkHttpTracingInterceptor;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmRequestIdProviderInterceptor;
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

    @Inject
    private CmOkHttpTracingInterceptor cmOkHttpTracingInterceptor;

    @Inject
    private CmRequestIdProviderInterceptor cmRequestIdProviderInterceptor;

    private ApiClient initializeApiClient(Cluster cluster) throws ClouderaManagerClientInitException {
        HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster.getStackCrn(),
                cluster.getClusterManager().getHost(), cluster.getTunnel());
        ClusterManager cm = cluster.getClusterManager();
        String user = secretService.get(cm.getUser());
        String pass = secretService.get(cm.getPass());

        ApiClient client = clouderaManagerApiClientProvider.getV31Client(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
        client.getHttpClient().interceptors().add(cmOkHttpTracingInterceptor);
        client.getHttpClient().interceptors().add(cmRequestIdProviderInterceptor);

        return client;
    }

    public boolean isClusterManagerRunning(Cluster cluster) {
        try {
            ApiClient apiClient = initializeApiClient(cluster);
            clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient).getVersion();
            return true;
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Failed to get version info from CM");
            return false;
        }
    }

    public Map<String, ApiConfig> getRoleConfigPropertiesFromCM(Cluster cluster, String serviceName,
            String roleGroupRef, Set roleConfigPropertyNames) {
        LOGGER.debug("Retrieving roleConfigProperties for cluster '{}', service '{}', roleGroupRef '{}'",
                cluster.getStackCrn(), serviceName, roleGroupRef);

        Map<String, ApiConfig> roleConfigProperties = requestLogging.logResponseTime(() -> {
            try {
                ApiClient client = initializeApiClient(cluster);
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

    public ApiHealthSummary getRoleHealthStatusFromCM(Cluster cluster, String serviceName, String serviceHealthCheckName) {
        return requestLogging.logResponseTime(() -> {
            try {
                ApiClient client = initializeApiClient(cluster);
                ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);

                ApiService service = servicesResourceApi.readService(cluster.getStackName(), serviceName, DataView.FULL_WITH_HEALTH_CHECK_EXPLANATION.name());

                Set<String> healthChecksNames = service.getHealthChecks().stream().map(ApiHealthCheck::getName).collect(Collectors.toSet());

                return service.getHealthChecks().stream()
                        .filter(apiHealthCheck -> serviceHealthCheckName.equalsIgnoreCase(apiHealthCheck.getName()))
                        .findFirst()
                        .map(ApiHealthCheck::getSummary)
                        .orElseThrow(() -> new RuntimeException(String.format("No health check found for service '%s', health check name '%s' for cluster '%s'",
                                serviceName, serviceHealthCheckName, cluster.getStackCrn())));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error during CM readHosts API call for cluster: %s", cluster.getStackCrn()), e);
            }
        }, String.format("getHealthSummaryForServiceFromCM for cluster crn '%s'", cluster.getStackCrn()));
    }

}
