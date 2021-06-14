package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelStatus;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
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
    private ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    @Inject
    private ClouderaManagerConfigService configService;

    @Inject
    private ClouderaManagerParcelDecommissionService clouderaManagerParcelDecommissionService;

    @Inject
    private ClouderaManagerParcelManagementService clouderaManagerParcelManagementService;

    @Inject
    private ClouderaManagerUpgradeService clouderaManagerUpgradeService;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Inject
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

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
            apiClient = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
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
                List<ApiHost> hosts = clouderaManagerApiFactory.getHostsResourceApi(apiClient).readHosts(null, null, SUMMARY).getItems();
                ApiHostRefList body = createUpscaledHostRefList(upscaleHostNames, hosts);
                clustersResourceApi.addHosts(clusterName, body);
                activateParcels(clustersResourceApi);
                applyHostGroupRolesOnUpscaledHosts(body, hostGroup.getName());
            } else {
                redistributeParcelsForRecovery();
                activateParcels(clustersResourceApi);
                clouderaManagerRoleRefreshService.refreshClusterRoles(apiClient, stack);
            }
            return instanceMetaDatas.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
        } catch (ApiException e) {
            LOGGER.warn("Failed to upscale: {}", e.getResponseBody(), e);
            throw new CloudbreakException("Failed to upscale", e);
        }
    }

    @Override
    public void cleanupCluster(Telemetry telemetry) {
        if (telemetry != null && telemetry.getWorkloadAnalytics() != null) {
            if (StackType.DATALAKE.equals(stack.getType())) {
                LOGGER.info("Stack type is datalake, no need for WA cleanup");
            } else {
                databusService.cleanUpMachineUser(stack);
            }
        }
    }

    @Override
    public void upgradeClusterRuntime(Set<ClusterComponent> components, boolean patchUpgrade) throws CloudbreakException {
        try {
            LOGGER.info("Starting to upgrade cluster runtimes. Patch upgrade: {}", patchUpgrade);
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);

            startClouderaManager();
            checkParcelApiAvailability();

            Set<ClouderaManagerProduct> products = getProducts(components);
            setParcelRepo(products, clouderaManagerResourceApi);
            refreshParcelRepos(clouderaManagerResourceApi);
            if (patchUpgrade) {
                downloadAndActivateParcels(products, parcelResourceApi);
                restartServices(clustersResourceApi);
            } else {
                ClouderaManagerProduct cdhProduct = getCdhProducts(products);
                upgradeNonCdhProducts(products, cdhProduct.getName(), parcelResourceApi);
                upgradeCdh(clustersResourceApi, parcelResourceApi, cdhProduct);
                restartStaleServices(clustersResourceApi);
            }
            removeUnusedParcelVersions(parcelResourceApi, products);
            configService.enableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, apiClient, stack.getName());
            LOGGER.info("Cluster runtime upgrade finished");
        } catch (ApiException e) {
            LOGGER.info("Could not upgrade Cloudera Runtime services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void upgradeCdh(ClustersResourceApi clustersResourceApi, ParcelResourceApi parcelResourceApi, ClouderaManagerProduct cdhService)
            throws ApiException, CloudbreakException {
        downloadParcels(Collections.singleton(cdhService), parcelResourceApi);
        distributeParcels(Collections.singleton(cdhService), parcelResourceApi);
        callUpgradeCdhCommand(cdhService, clustersResourceApi);
    }

    private Set<ClouderaManagerProduct> getProducts(Set<ClusterComponent> components) {
        return clouderaManagerProductsProvider.getProducts(components);
    }

    private void upgradeNonCdhProducts(Set<ClouderaManagerProduct> products, String cdhServiceName, ParcelResourceApi parcelResourceApi)
            throws CloudbreakException, ApiException {
        Set<ClouderaManagerProduct> nonCdhServices = getNonCdhProducts(products, cdhServiceName);
        if (!nonCdhServices.isEmpty()) {
            List<String> productNames = nonCdhServices.stream().map(ClouderaManagerProduct::getName).collect(Collectors.toList());
            LOGGER.debug("Starting to upgrade the following Non-CDH products: {}", productNames);
            downloadAndActivateParcels(nonCdhServices, parcelResourceApi);
        } else {
            LOGGER.debug("Skipping Non-CDH products upgrade because the cluster does not contains any other products beside CDH.");
        }
    }

    private void downloadAndActivateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi)
            throws ApiException, CloudbreakException {
        downloadParcels(products, parcelResourceApi);
        distributeParcels(products, parcelResourceApi);
        activateParcels(products, parcelResourceApi);
    }

    private ClouderaManagerProduct getCdhProducts(Set<ClouderaManagerProduct> products) {
        return products.stream()
                .filter(service -> service.getName().equals(com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH.name()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    private Set<ClouderaManagerProduct> getNonCdhProducts(Set<ClouderaManagerProduct> products, String cdhProductName) {
        return products.stream()
                .filter(product -> !product.getName().equals(cdhProductName))
                .collect(Collectors.toSet());
    }

    private void checkParcelApiAvailability() throws CloudbreakException {
        clouderaManagerParcelManagementService.checkParcelApiAvailability(stack, apiClient);
    }

    private void setParcelRepo(Set<ClouderaManagerProduct> products, ClouderaManagerResourceApi clouderaManagerResourceApi) throws ApiException {
        clouderaManagerParcelManagementService.setParcelRepos(products, clouderaManagerResourceApi);
    }

    private void refreshParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi) {
        clouderaManagerParcelManagementService.refreshParcelRepos(clouderaManagerResourceApi, stack, apiClient);
    }

    private void callUpgradeCdhCommand(ClouderaManagerProduct cdhProduct, ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_UPGRADE);
        clouderaManagerUpgradeService.callUpgradeCdhCommand(cdhProduct.getVersion(), clustersResourceApi, stack, apiClient);
    }

    private void distributeParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi) throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DISTRIBUTE_PARCEL);
        clouderaManagerParcelManagementService.distributeParcels(products, parcelResourceApi, stack, apiClient);
    }

    private void activateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi) throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_ACTIVATE_PARCEL);
        clouderaManagerParcelManagementService.activateParcels(products, parcelResourceApi, stack, apiClient);
    }

    private void downloadParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi) throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_PARCEL);
        clouderaManagerParcelManagementService.downloadParcels(products, parcelResourceApi, stack, apiClient);
    }

    private void removeUnusedParcelVersions(ParcelResourceApi parcelResourceApi, Set<ClouderaManagerProduct> products) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(apiClient);
        for (ClouderaManagerProduct product : products) {
            LOGGER.info("Removing unused {} parcels.", product.getName());
            clouderaManagerParcelDecommissionService.removeUnusedParcelVersions(apiClient, parcelsResourceApi, parcelResourceApi, stack, product.getName(),
                    product.getVersion());
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
        List<ApiHost> hostRefs = clustersResourceApi.listHosts(clusterName, null, null, null).getItems();
        return hostRefs.stream().map(ApiHost::getHostname).collect(Collectors.toList());
    }

    private void redistributeParcelsForRecovery() throws ApiException, CloudbreakException {
        LOGGER.debug("Refreshing parcel repos");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiCommand refreshParcelRepos = clouderaManagerResourceApi.refreshParcelRepos();
        PollingResult activateParcelsPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient,
                refreshParcelRepos.getId());
        handlePollingResult(activateParcelsPollingResult, "Cluster was terminated while waiting for parcel repository refresh",
                "Timeout while Cloudera Manager was refreshing parcel repositories.");

        PollingResult downloadPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient,
                refreshParcelRepos.getId());
        handlePollingResult(downloadPollingResult, "Cluster was terminated while waiting for parcel download",
                "Timeout while Cloudera Manager was downloading parcels.");
        LOGGER.debug("Refreshed parcel repos");
    }

    public void restartStaleServices(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        MgmtServiceResourceApi mgmtServiceResourceApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient);
        restartClouderaManagementServices(mgmtServiceResourceApi);
        deployConfigAndRefreshCMStaleServices(clustersResourceApi);
    }

    @VisibleForTesting
    void deployConfigAndRefreshCMStaleServices(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Redeploying client configurations and refreshing stale services in Cloudera Manager.");
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        List<ApiService> services = servicesResourceApi.readServices(stack.getName(), SUMMARY).getItems();
        List<ApiService> notFreshServices = getNotFreshServices(services, ApiService::getConfigStalenessStatus);
        List<ApiService> notFreshClientServices = getNotFreshServices(services, ApiService::getClientConfigStalenessStatus);
        if (!notFreshServices.isEmpty() || !notFreshClientServices.isEmpty()) {
            deployConfigAndRefreshStaleServices(clustersResourceApi, notFreshServices, notFreshClientServices);
        } else {
            LOGGER.debug("No stale services found in Cloudera Manager.");
        }
    }

    private List<ApiService> getNotFreshServices(List<ApiService> services, Function<ApiService, ApiConfigStalenessStatus> status) {
        return services.stream()
                .filter(service -> !ApiConfigStalenessStatus.FRESH.equals(status.apply(service)))
                .collect(Collectors.toList());
    }

    private void deployConfigAndRefreshStaleServices(ClustersResourceApi clustersResourceApi, List<ApiService> notFreshServices,
            List<ApiService> notFreshClientServices) throws ApiException, CloudbreakException {
        LOGGER.debug("Services with config staleness status: {}", notFreshServices.stream()
                .map(it -> it.getName() + ": " + it.getConfigStalenessStatus())
                .collect(Collectors.joining(", ")));
        LOGGER.debug("Services with client config staleness status: {}", notFreshClientServices.stream()
                .map(it -> it.getName() + ": " + it.getClientConfigStalenessStatus())
                .collect(Collectors.joining(", ")));
        List<ApiCommand> commands = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY).getItems();
        BigDecimal deployCommandId = clouderaManagerCommonCommandService.getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
        pollDeployConfig(deployCommandId);
        ApiCommand refreshServicesCmd = clouderaManagerCommonCommandService.getApiCommand(
                commands, "RefreshCluster", stack.getName(), clustersResourceApi::refresh);
        pollRefresh(refreshServicesCmd);
        LOGGER.debug("Config deployed and stale services are refreshed in Cloudera Manager.");
    }

    private void restartServices(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        ApiCommandList apiCommandList = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY);
        Optional<ApiCommand> optionalRestartCommand = apiCommandList.getItems().stream()
                .filter(cmd -> "Restart".equals(cmd.getName())).findFirst();
        ApiCommand restartCommand;
        if (optionalRestartCommand.isPresent()) {
            restartCommand = optionalRestartCommand.get();
            LOGGER.debug("Restart for Cluster services is already running with id: [{}]", restartCommand.getId());
        } else {
            LOGGER.info("Restarting cluster services.");
            ApiRestartClusterArgs restartClusterArgs = new ApiRestartClusterArgs();
            restartClusterArgs.setRedeployClientConfiguration(true);
            restartCommand = clustersResourceApi.restartCommand(stack.getName(), restartClusterArgs);
        }
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, restartCommand.getId());
        handlePollingResult(pollingResult, "Cluster was terminated while restarting services.", "Timeout happened while restarting services.");
    }

    private void restartClouderaManagementServices(MgmtServiceResourceApi mgmtServiceResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Restarting Cloudera Management Services in Cloudera Manager.");
        ApiCommandList apiCommandList = mgmtServiceResourceApi.listActiveCommands(SUMMARY);
        Optional<ApiCommand> optionalRestartCommand = apiCommandList.getItems().stream()
                .filter(cmd -> "Restart".equals(cmd.getName())).findFirst();
        ApiCommand restartCommand;
        if (optionalRestartCommand.isPresent()) {
            restartCommand = optionalRestartCommand.get();
            LOGGER.debug("Restart for CMS is already running with id: [{}]", restartCommand.getId());
        } else {
            restartCommand = mgmtServiceResourceApi.restartCommand();
        }
        pollRestart(restartCommand);
        LOGGER.debug("Restarted Cloudera Management Services in Cloudera Manager.");
    }

    private void pollRestart(ApiCommand restartCommand) throws CloudbreakException {
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(
                stack, apiClient, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for service restart",
                "Timeout while Cloudera Manager was restarting services.");
    }

    @VisibleForTesting
    void pollDeployConfig(BigDecimal commandId) throws CloudbreakException {
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(
                stack, apiClient, commandId);
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for config deploy",
                "Timeout while Cloudera Manager was config deploying services.");
    }

    @VisibleForTesting
    void pollRefresh(ApiCommand restartCommand) throws CloudbreakException {
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(
                stack, apiClient, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for service refresh",
                "Timeout while Cloudera Manager was refreshing services.");
    }

    private void applyHostGroupRolesOnUpscaledHosts(ApiHostRefList body, String hostGroupName) throws ApiException, CloudbreakException {
        LOGGER.debug("Applying host template on upscaled hosts. Host group: [{}]", hostGroupName);
        HostTemplatesResourceApi templatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(apiClient);
        ApiCommand applyHostTemplateCommand = templatesResourceApi.applyHostTemplate(stack.getName(), hostGroupName, START_ROLES_ON_UPSCALED_NODES, body);
        PollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(
                stack, apiClient, applyHostTemplateCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for host template to apply",
                "Timeout while Cloudera Manager was applying host template.");
        LOGGER.debug("Applied host template on upscaled hosts.");
    }

    private void activateParcels(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Deploying client configurations on upscaled hosts.");
        List<ApiCommand> commands = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY).getItems();
        BigDecimal deployCommandId = clouderaManagerCommonCommandService.getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
        pollDeployConfig(deployCommandId);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient, deployCommandId);
        handlePollingResult(pollingResult, "Cluster was terminated while waiting for parcels activation", "Timeout while Cloudera Manager activate parcels.");
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
    public void stopCluster(boolean disableKnoxAutorestart) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            LOGGER.debug("Stopping all Cloudera Runtime services");
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
            disableKnoxAutorestart(disableKnoxAutorestart);
            Collection<ApiService> apiServices = readServices(stack);
            boolean anyServiceNotStopped = apiServices.stream()
                    .anyMatch(service -> !ApiServiceState.STOPPED.equals(service.getServiceState())
                            && !ApiServiceState.STOPPING.equals(service.getServiceState()));
            if (anyServiceNotStopped) {
                ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
                PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, apiClient, apiCommand.getId());
                handlePollingResult(pollingResult, "Cluster was terminated while waiting for Hadoop services to stop",
                        "Timeout while stopping Cloudera Manager services.");
            }
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
        } catch (ApiException e) {
            LOGGER.info("Couldn't stop Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void disableKnoxAutorestart(boolean disableKnoxAutorestart) {
        if (disableKnoxAutorestart) {
            configService.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, apiClient, stack.getName());
        }
    }

    private Collection<ApiService> readServices(Stack stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        return api.readServices(stack.getName(), SUMMARY).getItems();
    }

    @Override
    public int startCluster() throws CloudbreakException {
        try {
            startClouderaManager();
            startAgents();
            // fetch th context
            // configure mgmt services.
            return startServices();
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }
//
//    private ClusterSetupService getClusterSetupService(Stack stack) {
//        return clusterApiConnectors.getConnector(stack)
//                .clusterSetupService();
//    }

    public void startClusterMgmtServices() throws CloudbreakException {
        startClouderaManager();
        startAgents();
    }

    public int startClusterServices() throws CloudbreakException {
        try {
            return startServices();
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

    @Override
    public Map<String, String> gatherInstalledParcels(String stackName) {
        return clouderaManagerParcelDecommissionService.getParcelsInStatus(clouderaManagerApiFactory.getParcelsResourceApi(apiClient), stackName,
                ParcelStatus.ACTIVATED);
    }

    @Override
    public void removeUnusedParcels(Set<ClusterComponent> usedParcelComponents) {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(apiClient);
        ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
        Map<String, ClouderaManagerProduct> cmProducts = new HashMap<>();
        for (ClusterComponent clusterComponent : usedParcelComponents) {
            ClouderaManagerProduct product = clusterComponent.getAttributes().getSilent(ClouderaManagerProduct.class);
            cmProducts.put(product.getName(), product);
        }
        clouderaManagerParcelDecommissionService.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(), cmProducts);
        clouderaManagerParcelDecommissionService.undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, cmProducts);
        clouderaManagerParcelDecommissionService.removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, cmProducts);
    }

    private int startServices() throws ApiException, CloudbreakException {
        Cluster cluster = stack.getCluster();
        ClustersResourceApi apiInstance = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        String clusterName = cluster.getName();
        LOGGER.debug("Starting all services for cluster.");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTING);
        Collection<ApiService> apiServices = readServices(stack);
        boolean anyServiceNotStarted = apiServices.stream()
                .anyMatch(service -> !ApiServiceState.STARTED.equals(service.getServiceState())
                        && !ApiServiceState.STARTING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()));
        ApiCommand apiCommand = null;
        if (anyServiceNotStarted) {
            apiCommand = apiInstance.startCommand(clusterName);
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient, apiCommand.getId());
            handlePollingResult(pollingResult, "Cluster was terminated while waiting for Cloudera Runtime services to start",
                    "Timeout while stopping Cloudera Manager services.");
        }
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTED);
        return apiCommand == null ? 0 : apiCommand.getId().intValue();
    }

    private void startAgents() {
        LOGGER.debug("Starting Cloudera Manager agents on the hosts.");
        PollingResult hostsJoinedResult = clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, apiClient);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Cloudera Manager agents.");
        }
    }

    private void startClouderaManager() throws CloudbreakException {
        PollingResult healthCheckResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient);
        handlePollingResult(healthCheckResult, "Cluster was terminated while waiting for Cloudera Manager to start.",
                "Cloudera Manager server was not restarted properly.");
    }

    @Override
    public void restartAll(boolean withMgmtServices) {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            restartServices(clustersResourceApi);
            if (withMgmtServices) {
                restartClouderaManagementServices(clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient));
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Could not restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void handlePollingResult(PollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        pollingResultErrorHandler.handlePollingResult(pollingResult, cancellationMessage, timeoutMessage);
    }

    @Override
    public void updateServiceConfigAndRestartService(String serviceType, String configName, String newConfigValue) throws Exception {
        configService.modifyServiceConfigValue(apiClient, stack.getName(), serviceType, configName, newConfigValue);
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        restartStaleServices(clustersResourceApi);
    }

}
