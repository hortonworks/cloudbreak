package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.google.api.client.util.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.healthcheck.ClouderaManagerHostHealthCheck;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Service
public class ClouderaManagerHealthService {

    public static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    public static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.DISABLED,
            ApiHealthSummary.NOT_AVAILABLE,
            ApiHealthSummary.HISTORY_NOT_AVAILABLE
    );

    public static final String FULL_WITH_EXPLANATION_VIEW = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    public static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final String FULL_VIEW = "FULL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHealthService.class);

    private static final String DUPLICATED_HOST = "This host is duplicated in Cloudera Manager, please check it in Cloudera Manager.";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private Set<ClouderaManagerHostHealthCheck> hostHealthChecks;

    private static <T> Map<HostName, T> convertHealthSummary(Map<HostName, ApiHealthSummary> hostHealth, Function<ApiHealthSummary, T> converter) {
        Map<HostName, T> result = new HashMap<>();
        hostHealth.forEach((hostname, healthSummary) -> result.put(hostname, converter.apply(healthSummary)));
        return result;
    }

    public ExtendedHostStatuses getExtendedHostStatuses(ApiClient client, Optional<String> runtimeVersion) {
        List<ApiHost> apiHostList = readHostsFromClouderaManager(client);
        List<ApiService> apiServiceList = readServicesOfAllClustersFromClouderaManager(client, apiHostList);
        Map<HostName, Set<HealthCheck>> hostStates = Maps.newHashMap();
        apiHostList.forEach(apiHost ->
            hostStates.merge(
                    hostName(apiHost.getHostname()),
                    getHealthChecks(runtimeVersion, apiHost, apiServiceList),
                    (k, v) -> Set.of(HealthCheck.unhealthy(HealthCheckType.HOST, DUPLICATED_HOST))));
        hostStates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        LOGGER.debug("Creating 'ExtendedHostStatuses' with {}", hostStates);
        return new ExtendedHostStatuses(hostStates);
    }

    public Map<HostName, String> getHostStatusesRaw(ApiClient client) {
        return convertHealthSummary(getHostHealthSummary(client), ApiHealthSummary::getValue);
    }

    private List<ApiHost> readHostsFromClouderaManager(ApiClient client) {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList apiHostList = api.readHosts(null, null, FULL_WITH_EXPLANATION_VIEW);
            LOGGER.trace("Response from CM for readHosts call: {}", apiHostList);
            return apiHostList.getItems();
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    private List<ApiService> readServicesOfAllClustersFromClouderaManager(ApiClient client, List<ApiHost> apiHostList) {
        Set<String> clusterNames = apiHostList.stream()
                .map(apiHost -> apiHost.getClusterRef().getClusterName())
                .collect(Collectors.toSet());
        return clusterNames.stream()
                .flatMap(clusterName -> readServicesOfClusterFromClouderaManager(client, clusterName).stream())
                .toList();
    }

    private List<ApiService> readServicesOfClusterFromClouderaManager(ApiClient client, String clusterName) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        try {
            ApiServiceList apiServiceList = servicesResourceApi.readServices(clusterName, ClouderaManagerConstants.SUMMARY);
            LOGGER.trace("Response from CM for readServices call: {}", apiServiceList);
            return apiServiceList.getItems();
        } catch (ApiException e) {
            LOGGER.info("Failed to get services from CM for cluster {}", clusterName, e);
            throw new RuntimeException("Failed to get services from CM due to: " + e.getMessage(), e);
        }
    }

    private Set<HealthCheck> getHealthChecks(Optional<String> runtimeVersion, ApiHost apiHost, List<ApiService> apiServiceList) {
        Set<String> servicesOnHost = apiHost.getRoleRefs().stream()
                .map(ApiRoleRef::getServiceName)
                .collect(Collectors.toSet());
        List<ApiService> hostServiceList = apiServiceList.stream()
                .filter(apiService -> servicesOnHost.contains(apiService.getName()))
                .toList();
        return hostHealthChecks.stream()
                .map(healthCheck -> healthCheck.getHealthCheck(runtimeVersion, apiHost, hostServiceList))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Collects summary of HOST_SCM_HEALTH check for each host.
     * Currently this is the best indicator of host availability.
     */
    private Map<HostName, ApiHealthSummary> getHostHealthSummary(ApiClient client) {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            return api.readHosts(null, null, FULL_VIEW).getItems().stream()
                    .filter(host -> host.getHealthChecks() != null)
                    .flatMap(host -> host.getHealthChecks().stream()
                            .filter(check -> ClouderaManagerHealthService.HOST_SCM_HEALTH.equals(check.getName()))
                            .map(ApiHealthCheck::getSummary)
                            .filter(healthSummary -> !ClouderaManagerHealthService.IGNORED_HEALTH_SUMMARIES.contains(healthSummary))
                            .map(healthSummary -> Pair.of(hostName(host.getHostname()), healthSummary))
                    )
                    .collect(toMap(Pair::getLeft, Pair::getRight));
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }
}
