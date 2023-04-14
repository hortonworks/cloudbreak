package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedCertHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedHostHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedServicesHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.HostCommissionState;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterHealthService implements ClusterHealthService {

    static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    static final String FULL_WITH_HEALTH_CHECK_EXPLANATION = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterHealthService.class);

    private static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.DISABLED,
            ApiHealthSummary.NOT_AVAILABLE,
            ApiHealthSummary.HISTORY_NOT_AVAILABLE
    );

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private StackDtoDelegate stack;

    private ApiClient apiClient;

    private HttpClientConfig httpClientConfig;

    public ClouderaManagerClusterHealthService(StackDtoDelegate stack, HttpClientConfig httpClientConfig) {
        this.stack = stack;
        this.httpClientConfig = httpClientConfig;
    }

    @PostConstruct
    private void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            apiClient = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, httpClientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public boolean isClusterManagerRunning() {
        try {
            clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient).getVersion();
            return true;
        } catch (ApiException e) {
            LOGGER.info("Failed to get version info from CM");
            return false;
        }
    }

    @Override
    public DetailedHostStatuses getDetailedHostStatuses(Optional<String> runtimeVersion) {
        List<ApiHost> apiHosts = getHostsFromCM();
        Map<HostName, Optional<DetailedHostHealthCheck>> hostsHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                ClouderaManagerClusterHealthService::getDetailedHostHealthCheck));

        Map<HostName, Optional<DetailedServicesHealthCheck>> servicesHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                apiHost -> getDetailedServicesHealthCheck(apiHost, runtimeVersion)));

        Map<HostName, Optional<DetailedCertHealthCheck>>  certHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                ClouderaManagerClusterHealthService::getDetailedCertificateCheck));

        DetailedHostStatuses detailedHostStatuses = new DetailedHostStatuses(hostsHealth, servicesHealth, certHealth);

        LOGGER.debug("Creating 'DetailedHostStatuses' with {}", detailedHostStatuses);

        return detailedHostStatuses;
    }

    private List<ApiHost> getHostsFromCM() {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        try {
            ApiHostList apiHostList = hostsResourceApi.readHosts(null, null, FULL_WITH_HEALTH_CHECK_EXPLANATION);
            LOGGER.info("Retrieved HostsList from CM HostsResourceApi readHosts call: {}", apiHostList);
            return apiHostList.getItems();
        } catch (ApiException e) {
            LOGGER.error("Error when reading hosts from CM: {}", e);
            throw new RuntimeException(e);
        }
    }

    private static Optional<DetailedHostHealthCheck> getDetailedHostHealthCheck(ApiHost apiHost) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_SCM_HEALTH.equals(health.getName()))
                .filter(health -> !IGNORED_HEALTH_SUMMARIES.contains(health.getSummary()))
                .findFirst()
                .map(apiHealthCheck -> new DetailedHostHealthCheck(healthSummaryToHealthCheckResult(apiHealthCheck.getSummary()),
                        Boolean.TRUE.equals(apiHost.getMaintenanceMode()),
                        HostCommissionState.valueOf(apiHost.getCommissionState().getValue()), apiHost.getLastHeartbeat()));
    }

    private static Optional<DetailedServicesHealthCheck> getDetailedServicesHealthCheck(ApiHost apiHost, Optional<String> runtimeVersion) {
        if (CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion)) {
            Set<String> servicesWithBadHealth = emptyIfNull(apiHost.getRoleRefs()).stream()
                    .filter(roleRef -> Objects.nonNull(roleRef.getHealthSummary()))
                    .filter(roleRef -> HealthCheckResult.UNHEALTHY.equals(healthSummaryToHealthCheckResult(roleRef.getHealthSummary())))
                    .map(ApiRoleRef::getServiceName)
                    .collect(Collectors.toSet());

            Set<String> servicesNotRunning = emptyIfNull(apiHost.getRoleRefs()).stream()
                    .filter(roleRef -> Objects.nonNull(roleRef.getRoleStatus()))
                    .filter(roleRef -> isRoleStopped(roleRef.getRoleStatus()))
                    .map(ApiRoleRef::getServiceName)
                    .collect(Collectors.toSet());

            return Optional.of(new DetailedServicesHealthCheck(servicesWithBadHealth, servicesNotRunning));
        }
        return Optional.empty();
    }

    private static Optional<DetailedCertHealthCheck> getDetailedCertificateCheck(ApiHost apiHost) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_AGENT_CERTIFICATE_EXPIRY.equals(health.getName()))
                .findFirst()
                .map(apiHealthCheck ->
                        new DetailedCertHealthCheck(healthSummaryToHealthCheckResult(apiHealthCheck.getSummary()), apiHealthCheck.getExplanation()));
    }

    private static HealthCheckResult healthSummaryToHealthCheckResult(ApiHealthSummary apiHealthSummary) {
        if (ApiHealthSummary.GOOD.equals(apiHealthSummary)) {
            return HealthCheckResult.HEALTHY;
        }
        return HealthCheckResult.UNHEALTHY;
    }

    private static boolean isRoleStopped(ApiRoleState apiRoleState) {
        switch (apiRoleState) {
            case STOPPED:
            case STOPPING:
                return true;
            default:
                return false;
        }
    }
}
