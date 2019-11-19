package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
@Scope("prototype")
public class ClouderaManagerModificationService implements ClusterModificationService {

    private static final String SUMMARY = "SUMMARY";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerModificationService.class);

    private static final Boolean START_ROLES_ON_UPSCALED_NODES = Boolean.TRUE;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private ClouderaManagerDatabusService databusService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient apiClient;

    public ClouderaManagerModificationService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            apiClient = clouderaManagerApiClientProvider.getClient(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public List<String> upscaleCluster(HostGroup hostGroup, Collection<InstanceMetaData> instanceMetaDatas)
            throws CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            String clusterName = stack.getName();
            List<String> upscaleHostNames = getUpscaleHosts(clustersResourceApi, clusterName, instanceMetaDatas);
            if (!upscaleHostNames.isEmpty()) {
                List<ApiHost> hosts = clouderaManagerApiFactory.getHostsResourceApi(apiClient).readHosts(SUMMARY).getItems();
                ApiHostRefList body = createUpscaledHostRefList(upscaleHostNames, hosts);
                clustersResourceApi.addHosts(clusterName, body);
                activateParcel(clustersResourceApi);
                applyHostGroupRolesOnUpscaledHosts(body, hostGroup.getName());
            } else {
                Long clusterId = stack.getCluster().getId();
                if (!isPrewarmed(clusterId)) {
                    redistributeParcelsForRecovery();
                }
                activateParcel(clustersResourceApi);
                clouderaManagerRoleRefreshService.refreshClusterRoles(apiClient, stack);
            }
            return instanceMetaDatas.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
        } catch (ApiException e) {
            LOGGER.warn("Failed to upscale: {}", e.getResponseBody(), e);
            throw new CloudbreakException("Failed to upscale", e);
        }
    }

    private List<String> getUpscaleHosts(ClustersResourceApi clustersResourceApi, String clusterName, Collection<InstanceMetaData> instanceMetaDatas)
            throws ApiException {
        List<String> clusterHosts = getHostNamesFromCM(clustersResourceApi, clusterName);
        return instanceMetaDatas.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .filter(hostname -> !clusterHosts.contains(hostname))
                .collect(Collectors.toList());
    }

    private List<String> getHostNamesFromCM(ClustersResourceApi clustersResourceApi, String clusterName) throws ApiException {
        List<ApiHostRef> hostRefs = clustersResourceApi.listHosts(clusterName).getItems();
        return hostRefs.stream().map(ApiHostRef::getHostname).collect(Collectors.toList());
    }

    private void redistributeParcelsForRecovery() throws ApiException, CloudbreakException {
        LOGGER.debug("Refreshing parcel repos");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiCommand refreshParcelRepos = clouderaManagerResourceApi.refreshParcelRepos();
        PollingResult activateParcelsPollingResult =
                clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient, refreshParcelRepos.getId());
        if (isExited(activateParcelsPollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcel repository refresh");
        } else if (isTimeout(activateParcelsPollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager was refreshing parcel repositories.");
        }

        PollingResult downloadPollingResult =
                clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient, refreshParcelRepos.getId());
        if (isExited(downloadPollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcel download");
        } else if (isTimeout(downloadPollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager was downloading parcels.");
        }
        LOGGER.debug("Refreshed parcel repos");
    }

    private Boolean isPrewarmed(Long clusterId) {
        return Optional.ofNullable(clusterComponentProvider.getClouderaManagerRepoDetails(clusterId))
                .map(ClouderaManagerRepo::getPredefined)
                .orElse(Boolean.FALSE);
    }

    public void restartStaleServices(MgmtServiceResourceApi mgmtServiceResourceApi, ClustersResourceApi clustersResourceApi)
            throws ApiException, CloudbreakException {
        restartClouderaManagementServices(mgmtServiceResourceApi);
        restartCMStaleServices(clustersResourceApi);
    }

    private void restartCMStaleServices(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Restarting stale services and redeploying client configurations in Cloudera Manager.");
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        List<ApiService> services = servicesResourceApi.readServices(stack.getName(), SUMMARY).getItems();
        boolean configStale = services.stream().anyMatch(service -> !ApiConfigStalenessStatus.FRESH.equals(service.getConfigStalenessStatus()));
        if (configStale) {
            Optional<ApiCommand> optionalRestartCommand = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY).getItems().stream()
                    .filter(cmd -> "RestartWaitingForStalenessSuccess".equals(cmd.getName())).findFirst();
            ApiCommand restartServicesCommand;
            if (optionalRestartCommand.isPresent()) {
                restartServicesCommand = optionalRestartCommand.get();
                LOGGER.debug("Restart for stale services is already running with id: [{}]", restartServicesCommand.getId());
            } else {
                restartServicesCommand = clustersResourceApi.restartCommand(stack.getName(),
                        new ApiRestartClusterArgs().redeployClientConfiguration(Boolean.TRUE).restartOnlyStaleServices(Boolean.TRUE));
            }
            pollRestart(restartServicesCommand);
            LOGGER.debug("Restarted stale services in Cloudera Manager.");
        } else {
            LOGGER.debug("No stale services found in Cloudera Manager.");
        }
    }

    private void restartClouderaManagementServices(MgmtServiceResourceApi mgmtServiceResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Restarting Cloudera Management Services in Cloudera Manager.");
        Optional<ApiCommand> optionalRestartCommand = mgmtServiceResourceApi.listActiveCommands(SUMMARY).getItems().stream()
                .filter(cmd -> "Restart".equals(cmd.getName())).findFirst();
        ApiCommand restartCommand;
        if (optionalRestartCommand.isPresent()) {
            restartCommand = optionalRestartCommand.get();
            LOGGER.debug("Restart for CMS is already running with id: [{}]", restartCommand.getId());
        } else {
            restartCommand = mgmtServiceResourceApi.restartCommand();
        }
        pollRestart(restartCommand);
        LOGGER.debug("Restarted Coudera Management Services in Cloudera Manager.");
    }

    private void pollRestart(ApiCommand restartCommand) throws CloudbreakException {
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(
                stack, apiClient, restartCommand.getId());
        if (isExited(hostTemplatePollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for service restart");
        } else if (isTimeout(hostTemplatePollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager was restarting services.");
        }
    }

    private void applyHostGroupRolesOnUpscaledHosts(ApiHostRefList body, String hostGroupName) throws ApiException, CloudbreakException {
        LOGGER.debug("Applying host template on upscaled hosts. Host group: [{}]", hostGroupName);
        HostTemplatesResourceApi templatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(apiClient);
        ApiCommand applyHostTemplateCommand = templatesResourceApi.applyHostTemplate(stack.getName(), hostGroupName, START_ROLES_ON_UPSCALED_NODES, body);
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(
                stack, apiClient, applyHostTemplateCommand.getId());
        if (isExited(hostTemplatePollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for host template to apply");
        } else if (isTimeout(hostTemplatePollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager was applying host template.");
        }
        LOGGER.debug("Applied host template on upscaled hosts.");
    }

    private void activateParcel(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Deploying client configurations on upscaled hosts.");
        ApiCommand deployCommand = clustersResourceApi.deployClientConfig(stack.getName());
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(
                stack, apiClient, deployCommand.getId());
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for client configurations to deploy");
        } else if (isTimeout(pollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager deployed client configurations.");
        }
        LOGGER.debug("Deployed client configurations on upscaled hosts.");
        pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient, deployCommand.getId());
        if (isExited(pollingResult)) {
            throw new CancellationException("Cluster was terminated while waiting for parcels activation");
        } else if (isTimeout(pollingResult)) {
            throw new CloudbreakException("Timeout while Cloudera Manager activate parcels.");
        }
        LOGGER.debug("Parcels are activated on upscaled hosts.");
    }

    private ApiHostRefList createUpscaledHostRefList(List<String> upscaleHostNames, List<ApiHost> hosts) {
        LOGGER.debug("Creating ApiHostRefList from upscaled hosts.");
        ApiHostRefList body = new ApiHostRefList();
        upscaleHostNames.forEach(hostname -> {
            hosts.stream()
                    .filter(host -> hostname.equalsIgnoreCase(host.getHostname()))
                    .findFirst().ifPresent(apiHost -> {
                String hostId = apiHost.getHostId();
                body.addItemsItem(
                        new ApiHostRef().hostname(hostname).hostId(hostId));
            });
        });
        LOGGER.debug("Created ApiHostRefList from upscaled hosts. Host count: [{}]", body.getItems().size());
        return body;
    }

    @Override
    public void stopCluster() throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            LOGGER.debug("Stop all Hadoop services");
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
            Collection<ApiService> apiServices = readServices(stack);
            boolean anyServiceNotStopped = apiServices.stream()
                    .anyMatch(service -> !ApiServiceState.STOPPED.equals(service.getServiceState())
                            && !ApiServiceState.STOPPING.equals(service.getServiceState()));
            if (anyServiceNotStopped) {
                ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
                PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, apiClient, apiCommand.getId());
                if (isExited(pollingResult)) {
                    throw new CancellationException("Cluster was terminated while waiting for Hadoop services to stop");
                } else if (isTimeout(pollingResult)) {
                    throw new CloudbreakException("Timeout while stopping Cloudera Manager services.");
                }
            }
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
        } catch (ApiException e) {
            LOGGER.info("Couldn't stop ClouderaManager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Collection<ApiService> readServices(Stack stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        return api.readServices(stack.getName(), SUMMARY).getItems();
    }

    @Override
    public int startCluster(Set<InstanceMetaData> hostsInCluster) throws CloudbreakException {
        try {
            startClouderaManager(stack, apiClient);
            startAgents(stack, apiClient);
            return startServices(stack, apiClient);
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName) {
        return null;
    }

    @Override
    public String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId) {
        return null;
    }

    private int startServices(Stack stack, ApiClient client) throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi apiInstance = new ClustersResourceApi(client);
        String clusterName = cluster.getName();
        LOGGER.debug("Starting all services for cluster.");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTING);
        Collection<ApiService> apiServices = readServices(stack);
        boolean anyServiceNotStarted = apiServices.stream()
                .anyMatch(service -> !ApiServiceState.STARTED.equals(service.getServiceState())
                        && !ApiServiceState.STARTING.equals(service.getServiceState()));
        ApiCommand apiCommand = null;
        if (anyServiceNotStarted) {
            apiCommand = apiInstance.startCommand(clusterName);
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, client, apiCommand.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakException("Timeout while stopping Cloudera Manager services.");
            }
        }
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTED);
        return apiCommand == null ? 0 : apiCommand.getId().intValue();
    }

    private void startAgents(Stack stack, ApiClient client) {
        LOGGER.debug("Starting Cloudera Manager agents on the hosts.");
        PollingResult hostsJoinedResult = clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, client);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Cloudera Manager agents.");
        }
    }

    private void startClouderaManager(Stack stack, ApiClient client) throws CloudbreakException {
        PollingResult healthCheckResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, client);
        if (isExited(healthCheckResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Cloudera Manager to start.");
        } else if (isTimeout(healthCheckResult)) {
            throw new CloudbreakException("Cloudera Manager server was not restarted properly.");
        }
    }

    @Override
    public void cleanupCluster(Telemetry telemetry) throws CloudbreakException {
        if (telemetry != null && telemetry.getWorkloadAnalytics() != null) {
            if (StackType.DATALAKE.equals(stack.getType())) {
                LOGGER.info("Stack type is datalake, no need for WA cleanup");
            } else {
                databusService.cleanUpMachineUser(stack);
            }
        }
    }

    @Override
    public Map<String, String> gatherInstalledComponents(String hostname) {
        return Map.of();
    }

    @Override
    public void stopComponents(Map<String, String> components, String hostname) {

    }

    @Override
    public void ensureComponentsAreStopped(Map<String, String> components, String hostname) {

    }

    @Override
    public void initComponents(Map<String, String> components, String hostname) {

    }

    @Override
    public void installComponents(Map<String, String> components, String hostname) {

    }

    @Override
    public void regenerateKerberosKeytabs(String hostname, KerberosConfig kerberosConfig) {

    }

    @Override
    public void startComponents(Map<String, String> components, String hostname) {

    }

    @Override
    public void restartAll() {

    }
}
