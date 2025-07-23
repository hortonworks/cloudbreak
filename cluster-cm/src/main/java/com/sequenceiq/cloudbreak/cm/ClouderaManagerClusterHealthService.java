package com.sequenceiq.cloudbreak.cm;

import static com.cloudera.api.swagger.model.ApiHealthSummary.BAD;
import static com.cloudera.api.swagger.model.ApiHealthSummary.CONCERNING;
import static com.cloudera.api.swagger.model.ApiHealthSummary.DISABLED;
import static com.cloudera.api.swagger.model.ApiHealthSummary.GOOD;
import static com.cloudera.api.swagger.model.ApiHealthSummary.HISTORY_NOT_AVAILABLE;
import static com.cloudera.api.swagger.model.ApiHealthSummary.NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.HEALTHY;
import static com.sequenceiq.cloudbreak.common.type.HealthCheckResult.UNHEALTHY;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.annotations.VisibleForTesting;
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
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterHealthService implements ClusterHealthService {

    private static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    private static final String FULL_WITH_HEALTH_CHECK_EXPLANATION = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    private static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final String NODE_MANAGER_CONNECTIVITY = "NODE_MANAGER_CONNECTIVITY";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterHealthService.class);

    private static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            DISABLED,
            NOT_AVAILABLE,
            HISTORY_NOT_AVAILABLE
    );

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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
    public Map<String, String> readServicesHealth(String stackName) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        try {
            List<ApiService> apiServiceList = servicesResourceApi.readServices(stackName, DataView.FULL.name()).getItems();
            Map<String, String> componentWithHealthCheck = apiServiceList.stream()
                    .flatMap(apiService -> apiService.getHealthChecks().stream())
                    .collect(Collectors.toMap(
                            apiHealthCheck -> apiHealthCheck.getName(),
                            apiHealthCheck -> apiHealthCheck.getSummary().toString()
                    ));
            return componentWithHealthCheck;
        } catch (ApiException e) {
            LOGGER.error("Error when reading services from CM", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
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
        List<ApiRole> apiRoles = getRolesFromCM(extractYarnServiceNameFromBlueprint(stack), null);
        Map<HostName, Optional<DetailedHostHealthCheck>> hostsHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                this::getDetailedHostHealthCheck));

        Map<HostName, Optional<DetailedServicesHealthCheck>> servicesHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                apiHost -> getDetailedServicesHealthCheck(apiHost, runtimeVersion, apiRoles)));

        Map<HostName, Optional<DetailedCertHealthCheck>>  certHealth = apiHosts.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()),
                this::getDetailedCertificateCheck));

        DetailedHostStatuses detailedHostStatuses = new DetailedHostStatuses(hostsHealth, servicesHealth, certHealth);

        LOGGER.debug("Creating 'DetailedHostStatuses' with {}", detailedHostStatuses);

        return detailedHostStatuses;
    }

    @Override
    public Set<String> getDisconnectedNodeManagers() {
        List<ApiRole> apiNodeManagerRoles = getNodemanagerRolesFromCM(stack);
        return collectDisconnectedNodeManagers(apiNodeManagerRoles);
    }

    private List<ApiHost> getHostsFromCM() {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        try {
            ApiHostList apiHostList = hostsResourceApi.readHosts(null, null, FULL_WITH_HEALTH_CHECK_EXPLANATION);
            LOGGER.info("Retrieved HostsList from CM HostsResourceApi readHosts call: {}", apiHostList);
            return apiHostList.getItems();
        } catch (ApiException e) {
            LOGGER.error("Error when reading hosts from CM", e);
            throw new RuntimeException(e);
        }
    }

    private List<ApiRole> getRolesFromCM(String serviceRefName, String queryFilter) {
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(apiClient);
        ApiRoleList roles = null;
        try {
            roles = rolesResourceApi.readRoles(stack.getName(), serviceRefName, queryFilter, FULL_WITH_HEALTH_CHECK_EXPLANATION);
            LOGGER.info("Fetched ApiRoleList from CM RolesResourceAPI readRoles call: {}", roles);
            return roles.getItems();
        } catch (ApiException e) {
            LOGGER.error("Error when reading roles from CM", e);
            throw new RuntimeException(e);
        }
    }

    private List<ApiRole> getNodemanagerRolesFromCM(StackDtoDelegate stack) {
        String yarnServiceName = extractYarnServiceNameFromBlueprint(stack);
        return emptyIfNull(getRolesFromCM(yarnServiceName, null)).stream()
                .filter(role -> YarnRoles.NODEMANAGER.equalsIgnoreCase(role.getType())).toList();
    }

    private Optional<DetailedHostHealthCheck> getDetailedHostHealthCheck(ApiHost apiHost) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_SCM_HEALTH.equals(health.getName()))
                .filter(health -> !IGNORED_HEALTH_SUMMARIES.contains(health.getSummary()))
                .findFirst()
                .map(apiHealthCheck -> new DetailedHostHealthCheck(healthSummaryToHealthCheckResult(apiHealthCheck.getSummary()),
                        Boolean.TRUE.equals(apiHost.isMaintenanceMode()),
                        HostCommissionState.valueOf(apiHost.getCommissionState().getValue()), apiHost.getLastHeartbeat()));
    }

    private Optional<DetailedServicesHealthCheck> getDetailedServicesHealthCheck(ApiHost apiHost, Optional<String> runtimeVersion, List<ApiRole> apiRoles) {
        if (CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion)) {
            Set<String> servicesRolesWithBadHealth = getServicesRolesWithBadHealth(apiHost, apiRoles);

            Set<String> servicesNotRunning = emptyIfNull(apiHost.getRoleRefs()).stream()
                    .filter(roleRef -> nonNull(roleRef.getRoleStatus()))
                    .filter(roleRef -> isRoleStopped(roleRef.getRoleStatus()))
                    .map(ApiRoleRef::getServiceName)
                    .collect(Collectors.toSet());

            return Optional.of(new DetailedServicesHealthCheck(servicesRolesWithBadHealth, servicesNotRunning));
        }
        return Optional.empty();
    }

    private Set<String> getServicesRolesWithBadHealth(ApiHost apiHost, List<ApiRole> apiRoles) {
        for (ApiRole apiRole : apiRoles) {
            if (apiRole.getHostRef().getHostname().equals(apiHost.getHostname())) {
                return collectUnhealthyHealthChecks(apiRole);
            }
        }
        return Collections.emptySet();
    }

    private Set<String> collectUnhealthyHealthChecks(ApiRole apiRole) {
        return emptyIfNull(apiRole.getHealthChecks()).stream().filter(hc -> BAD.equals(hc.getSummary()) ||
                CONCERNING.equals(hc.getSummary())).map(ApiHealthCheck::getName).collect(Collectors.toSet());
    }

    private Set<String> collectDisconnectedNodeManagers(List<ApiRole> allNMRoles) {
        return allNMRoles.stream()
                .filter(this::isNodeManagerDisconnected)
                .filter(nm -> nonNull(nm.getHostRef()))
                .map(nm -> nm.getHostRef().getHostname())
                .map(StringUtils::lowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isNodeManagerDisconnected(ApiRole role) {
        Predicate<ApiHealthCheck> disconnectedNMCheck = hc -> {
            ApiHealthSummary summary = hc.getSummary();
            String healthCheckName = hc.getName();
            // The connectivty health check is disabled if the nodes are decommissioned
            return NODE_MANAGER_CONNECTIVITY.equals(healthCheckName) && (BAD.equals(summary) || CONCERNING.equals(summary));
        };
        return emptyIfNull(role.getHealthChecks()).stream().anyMatch(disconnectedNMCheck);
    }

    private Optional<DetailedCertHealthCheck> getDetailedCertificateCheck(ApiHost apiHost) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_AGENT_CERTIFICATE_EXPIRY.equals(health.getName()))
                .findFirst()
                .map(apiHealthCheck ->
                        new DetailedCertHealthCheck(healthSummaryToHealthCheckResult(apiHealthCheck.getSummary()), apiHealthCheck.getExplanation()));
    }

    private HealthCheckResult healthSummaryToHealthCheckResult(ApiHealthSummary apiHealthSummary) {
        if (GOOD.equals(apiHealthSummary)) {
            return HEALTHY;
        }
        return UNHEALTHY;
    }

    private boolean isRoleStopped(ApiRoleState apiRoleState) {
        return switch (apiRoleState) {
            case STOPPED, STOPPING -> true;
            default -> false;
        };
    }

    @VisibleForTesting
    protected String extractYarnServiceNameFromBlueprint(StackDtoDelegate stack) {
        CmTemplateProcessor templateProcessor = cmTemplateProcessorFactory.get(stack.getBlueprint().getBlueprintJsonText());
        return templateProcessor.getServiceByType(YarnRoles.YARN).map(ApiClusterTemplateService::getRefName).orElse(null);
    }
}
