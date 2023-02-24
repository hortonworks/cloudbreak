package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_5_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_UPDATED_REMOTE_DATA_CONTEXT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_UPDATING_REMOTE_DATA_CONTEXT;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiCallback;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiEntityTag;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ScalingException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.URLUtils;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
@Scope("prototype")
public class ClouderaManagerModificationService implements ClusterModificationService {

    private static final String SUMMARY = "SUMMARY";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerModificationService.class);

    private static final Boolean START_ROLES_ON_UPSCALED_NODES = Boolean.TRUE;

    private static final String HOST_TEMPLATE_NAME_TAG = "_cldr_cm_host_template_name";

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
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private ClouderaManagerRestartService clouderaManagerRestartService;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    private ApiClient apiClient;

    public ClouderaManagerModificationService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            apiClient = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public List<String> upscaleCluster(Map<HostGroup, Set<InstanceMetaData>> instanceMetaDatasByHostGroup) throws CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDatasByHostGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Map<String, InstanceMetaData> upscaleInstancesMap = Maps.newHashMap();
        try {
            LOGGER.debug("Starting cluster upscale with hosts: {}.", instanceMetaDatas.stream()
                    .map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.joining(", ")));
            String clusterName = stack.getName();
            Set<String> clusterHostnames = getClusterHostnamesFromCM(clustersResourceApi, clusterName);
            List<ApiHost> hosts = getHostsFromCM();

            LOGGER.debug("Processing outdated cluster hosts. Cluster hostnames from CM: {}", clusterHostnames);
            setHostRackIdForOutdatedClusterHosts(instanceMetaDatas, clusterHostnames, hosts);

            LOGGER.debug("Processing upscaled cluster hosts.");
            upscaleInstancesMap = getInstancesMap(clusterHostnames, instanceMetaDatas, true);
            if (!upscaleInstancesMap.isEmpty()) {
                Map<String, ApiHost> upscaleHostsMap = getHostsMap(upscaleInstancesMap, hosts);
                setHostRackIdBatch(upscaleInstancesMap, upscaleHostsMap);
                ApiHostRefList bodyForAddHosts = createUpscaledHostRefList(upscaleInstancesMap, upscaleHostsMap);
                clustersResourceApi.addHosts(clusterName, bodyForAddHosts);
                activateParcels(clustersResourceApi);
                for (HostGroup hostGroup : instanceMetaDatasByHostGroup.keySet()) {
                    ApiHostRefList bodyForApplyHostGroupRoles = getBodyForApplyHostGroupRoles(upscaleInstancesMap, upscaleHostsMap, hostGroup);
                    applyHostGroupRolesOnUpscaledHosts(bodyForApplyHostGroupRoles, hostGroup.getName());
                }
            } else {
                redistributeParcelsForRecovery();
                activateParcels(clustersResourceApi);
            }
            deployClientConfig(clustersResourceApi, stack);
            clouderaManagerRoleRefreshService.refreshClusterRoles(apiClient, stack);
            LOGGER.debug("Cluster upscale completed.");
            return instanceMetaDatas.stream()
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            LOGGER.error(String.format("Failed to upscale. Response: %s", e.getResponseBody()), e);
            throw new ScalingException("Failed to upscale", e,
                    upscaleInstancesMap.values().stream().map(InstanceMetaData::getInstanceId).collect(Collectors.toSet()));
        }
    }

    private ApiHostRefList getBodyForApplyHostGroupRoles(Map<String, InstanceMetaData> upscaleInstancesMap, Map<String, ApiHost> upscaleHostsMap,
            HostGroup hostGroup) {
        Map<String, InstanceMetaData> upscaleInstancesMapForHostGroup =
                upscaleInstancesMap.entrySet().stream()
                        .filter(instanceEntry -> hostGroup.getName().equals(instanceEntry.getValue().getInstanceGroupName()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return createUpscaledHostRefList(upscaleInstancesMapForHostGroup, upscaleHostsMap);
    }

    private BigDecimal deployClientConfig(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack) throws ApiException, CloudbreakException {
        List<ApiCommand> commands = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY, null).getItems();
        return deployClientConfig(clustersResourceApi, stack, commands);
    }

    private BigDecimal deployClientConfig(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, List<ApiCommand> commands)
            throws ApiException, CloudbreakException {
        BigDecimal deployCommandId = clouderaManagerCommonCommandService.getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
        pollDeployConfig(deployCommandId);
        return deployCommandId;
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

    private void refreshRemoteDataContextFromDatalakeInCaseOfDatahub(Optional<String> remoteDataContext) {
        if (remoteDataContext.isPresent()) {
            ClouderaManagerSetupService clouderaManagerSetupService = applicationContext.getBean(ClouderaManagerSetupService.class, stack, clientConfig);
            clouderaManagerSetupService.setupRemoteDataContext(remoteDataContext.get());
        } else {
            LOGGER.warn("Remote Data Context update is not needed");
        }
    }

    @Override
    public void upgradeClusterRuntime(Set<ClusterComponentView> components, boolean patchUpgrade, Optional<String> remoteDataContext,
            boolean rollingUpgradeEnabled) throws CloudbreakException {
        try {
            LOGGER.info("Starting to upgrade cluster runtimes. Patch upgrade: {}", patchUpgrade);
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);

            startClusterMgmtServices();
            tagHostsWithHostTemplateName();
            checkParcelApiAvailability();
            disableKnoxAutorestart(rollingUpgradeEnabled);

            refreshRemoteDataContextFromDatalakeInCaseOfDatahub(remoteDataContext);

            Set<ClouderaManagerProduct> products = getProducts(components);
            setParcelRepo(products, clouderaManagerResourceApi);
            refreshParcelRepos(clouderaManagerResourceApi);
            restartMgmtServices();
            LOGGER.debug("Starting the upgrade for the new components: {}", products);
            if (patchUpgrade) {
                downloadAndActivateParcels(products, parcelResourceApi, true);
                startServices();
                callPostClouderaRuntimeUpgradeCommandIfCMIsNewerThan751(rollingUpgradeEnabled);
                restartServices(rollingUpgradeEnabled);
            } else {
                ClouderaManagerProduct cdhProduct = clouderaManagerProductsProvider.getCdhProducts(products);
                upgradeNonCdhProducts(products, cdhProduct.getName(), parcelResourceApi, true);
                upgradeCdh(clustersResourceApi, parcelResourceApi, cdhProduct, rollingUpgradeEnabled);
                startServices();
                deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);
            }
            removeUnusedParcelVersions(parcelResourceApi, products);
            enableKnoxAutoRestart();
            LOGGER.info("Cluster runtime upgrade finished");
        } catch (ApiException e) {
            LOGGER.error("Could not upgrade Cloudera Runtime services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void enableKnoxAutoRestart() {
        configService.enableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, apiClient, stack.getName());
    }

    private void tagHostsWithHostTemplateName() throws ApiException, CloudbreakException {
        ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_6_0)) {
            LOGGER.info("Tagging hosts after runtime upgrade.");
            ApiClient v46Client = buildv46ApiClient();
            HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(v46Client);
            Set<String> hostnamesFromCM = fetchHostNamesFromCm(v46Client);
            startAsyncTagCalls(hostsResourceApi, hostnamesFromCM);
        } else {
            LOGGER.info("Skipping host tagging. Cloudera Manager version needs to be equal or higher, than 7.6.0. Current version: [{}]",
                    clouderaManagerRepoDetails.getVersion());
        }
    }

    private void startAsyncTagCalls(HostsResourceApi hostsResourceApi, Set<String> hostnamesFromCM) {
        stack.getNotTerminatedInstanceMetaData()
                .stream()
                .filter(im -> hostnamesFromCM.contains(im.getDiscoveryFQDN()))
                .forEach(im -> asyncTagHost(im.getDiscoveryFQDN(), hostsResourceApi, im.getInstanceGroupName()));
    }

    private Set<String> fetchHostNamesFromCm(ApiClient v46Client) throws ApiException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(v46Client);
        return getClusterHostnamesFromCM(clustersResourceApi, stack.getName());
    }

    private ApiClient buildv46ApiClient() throws CloudbreakException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            return clouderaManagerApiClientProvider.getV46Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.error("Failed to init V46 client.", e);
            throw new CloudbreakException(e);
        }
    }

    private void asyncTagHost(String hostname, HostsResourceApi hostsResourceApi, String instanceGroupName) {
        ApiEntityTag tag = new ApiEntityTag().name(HOST_TEMPLATE_NAME_TAG).value(instanceGroupName);
        try {
            ApiCallback<List<ApiEntityTag>> callback = new ApiCallback<>() {
                @Override
                public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
                    LOGGER.error("Tagging failed for host [{}]: {}. Response headers: {}", hostname, e.getMessage(), map, e);
                    throw new ClouderaManagerOperationFailedException("Host tagging failed for host: " + hostname, e);
                }

                @Override
                public void onSuccess(List<ApiEntityTag> apiEntityTags, int i, Map<String, List<String>> map) {
                    LOGGER.debug("Tagging successful for host: [{}]. Body: {}, headers: {}", hostname, apiEntityTags, map);
                }

                @Override
                public void onUploadProgress(long l, long l1, boolean b) {
                }

                @Override
                public void onDownloadProgress(long l, long l1, boolean b) {
                }
            };
            LOGGER.debug("Tagging host [{}] with [{}]", hostname, tag);
            hostsResourceApi.addTagsAsync(hostname, List.of(tag), callback);
        } catch (ApiException e) {
            LOGGER.error("Error while tagging host: [{}]", hostname, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void updateParcelSettings(Set<ClouderaManagerProduct> products) throws CloudbreakException {
        try {
            checkParcelApiAvailability();
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
            setParcelRepo(products, clouderaManagerResourceApi);
            refreshParcelRepos(clouderaManagerResourceApi);
        } catch (ApiException e) {
            LOGGER.error("Error during updating parcel settings!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void downloadParcels(Set<ClouderaManagerProduct> products) throws CloudbreakException {
        try {
            LOGGER.debug("Downloading parcels: {}", products);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
            downloadParcels(products, parcelResourceApi);
        } catch (ApiException e) {
            LOGGER.error("Error during downloading parcels!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void distributeParcels(Set<ClouderaManagerProduct> products) throws CloudbreakException {
        try {
            LOGGER.debug("Distributing parcels: {}", products);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
            distributeParcels(products, parcelResourceApi);
        } catch (ApiException e) {
            LOGGER.error("Error during distributing parcels!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void upgradeCdh(ClustersResourceApi clustersResourceApi, ParcelResourceApi parcelResourceApi, ClouderaManagerProduct cdhService,
            boolean rollingUpgradeEnabled) throws ApiException, CloudbreakException {
        downloadParcels(Collections.singleton(cdhService), parcelResourceApi);
        distributeParcels(Collections.singleton(cdhService), parcelResourceApi);
        callUpgradeCdhCommand(cdhService, clustersResourceApi, rollingUpgradeEnabled);
    }

    private Set<ClouderaManagerProduct> getProducts(Set<ClusterComponentView> components) {
        return clouderaManagerProductsProvider.getProducts(components);
    }

    private void upgradeNonCdhProducts(Set<ClouderaManagerProduct> products, String cdhServiceName, ParcelResourceApi parcelResourceApi,
            boolean activateParcels) throws CloudbreakException, ApiException {
        Set<ClouderaManagerProduct> nonCdhServices = getNonCdhProducts(products, cdhServiceName);
        if (!nonCdhServices.isEmpty()) {
            List<String> productNames = nonCdhServices.stream().map(ClouderaManagerProduct::getName).collect(Collectors.toList());
            LOGGER.debug("Starting to upgrade the following Non-CDH products: {}", productNames);
            downloadAndActivateParcels(nonCdhServices, parcelResourceApi, activateParcels);
        } else {
            LOGGER.debug("Skipping Non-CDH products upgrade because the cluster does not contains any other products beside CDH.");
        }
    }

    private void downloadAndActivateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, boolean activate)
            throws ApiException, CloudbreakException {
        downloadParcels(products, parcelResourceApi);
        distributeParcels(products, parcelResourceApi);
        if (activate) {
            activateParcels(products, parcelResourceApi);
        } else {
            LOGGER.info("No parcel activation is necessary yet.");
        }
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

    private void callUpgradeCdhCommand(ClouderaManagerProduct cdhProduct, ClustersResourceApi clustersResourceApi, boolean rollingUpgradeEnabled)
            throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                rollingUpgradeEnabled ? ResourceEvent.CLUSTER_UPGRADE_START_ROLLING_UPGRADE : ResourceEvent.CLUSTER_UPGRADE_START_UPGRADE);
        clouderaManagerUpgradeService.callUpgradeCdhCommand(cdhProduct.getVersion(), clustersResourceApi, stack, apiClient, rollingUpgradeEnabled);
    }

    private void callPostClouderaRuntimeUpgradeCommandIfCMIsNewerThan751(boolean rollingUpgradeEnabled)
            throws ApiException, CloudbreakException {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        String currentCMVersion = clouderaManagerRepo.getVersion();
        Versioned baseCMVersion = CLOUDERAMANAGER_VERSION_7_5_1;
        if (isVersionNewerOrEqualThanLimited(currentCMVersion, baseCMVersion)) {
            LOGGER.debug("Cloudera Manager version {} is newer than {} hence calling post runtime upgrade command using /v45 API",
                    currentCMVersion, baseCMVersion.getVersion());
            eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
            clouderaManagerRestartService.waitForRestartExecutionIfPresent(apiClient, stack, rollingUpgradeEnabled);
            ClusterView cluster = stack.getCluster();
            String user = cluster.getCloudbreakAmbariUser();
            String password = cluster.getCloudbreakAmbariPassword();
            try {
                ApiClient v45Client = clouderaManagerApiClientProvider.getV45Client(stack.getGatewayPort(), user, password, clientConfig);
                ClustersResourceApi clustersResourceV45Api = clouderaManagerApiFactory.getClustersResourceApi(v45Client);
                clouderaManagerUpgradeService.callPostRuntimeUpgradeCommand(clustersResourceV45Api, stack, v45Client);
            } catch (ClouderaManagerClientInitException e) {
                LOGGER.info("Couldn't build CM v45 client", e);
                throw new CloudbreakException(e);
            }
        } else {
            LOGGER.debug("Cloudera Manager version {} is older than {} hence NOT calling post runtime upgrade command",
                    currentCMVersion, baseCMVersion.getVersion());
        }
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
            clouderaManagerParcelDecommissionService.removeUnusedParcelVersions(apiClient, parcelsResourceApi, parcelResourceApi, stack, product);
        }
    }

    private List<ApiHost> getHostsFromCM() throws ApiException {
        LOGGER.debug("Retrieving registered host details from CM.");
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        List<ApiHost> hosts = hostsResourceApi.readHosts(null, null, SUMMARY).getItems();
        LOGGER.debug("Retrieving registered host details from CM completed. Host count: [{}]", hosts.size());
        return hosts;
    }

    private Map<String, InstanceMetaData> getInstancesMap(Set<String> clusterHostnames, Collection<InstanceMetaData> instanceMetaDatas, boolean forUpscale) {
        LOGGER.debug("Creating map from instances.");

        Predicate<InstanceMetaData> instanceHostnamePredicate = instanceMetaData -> clusterHostnames.contains(instanceMetaData.getDiscoveryFQDN());
        if (forUpscale) {
            instanceHostnamePredicate = Predicate.not(instanceHostnamePredicate);
        }

        Map<String, InstanceMetaData> instancesMap = instanceMetaDatas.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceHostnamePredicate)
                .collect(Collectors.toMap(instanceMetaData -> instanceMetaData.getDiscoveryFQDN().toLowerCase(Locale.ROOT), Function.identity()));
        LOGGER.debug("Created map from instances. Instance count: [{}]", instancesMap.size());
        return instancesMap;
    }

    private Set<String> getClusterHostnamesFromCM(ClustersResourceApi clustersResourceApi, String clusterName) throws ApiException {
        LOGGER.debug("Retrieving cluster host references from CM.");
        List<ApiHost> hostRefs = clustersResourceApi.listHosts(clusterName, null, null, null).getItems();
        Set<String> clusterHostnames = hostRefs.stream()
                .map(ApiHost::getHostname)
                .collect(Collectors.toSet());
        LOGGER.debug("Retrieving cluster host references from CM completed. Host count: [{}]", clusterHostnames.size());
        return clusterHostnames;
    }

    private Map<String, ApiHost> getHostsMap(Map<String, InstanceMetaData> instancesMap, List<ApiHost> hosts) {
        LOGGER.debug("Creating map from hosts.");
        Map<String, ApiHost> hostsMap = hosts.stream()
                .filter(host -> instancesMap.containsKey(host.getHostname().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(host -> host.getHostname().toLowerCase(Locale.ROOT), Function.identity()));
        LOGGER.debug("Created map from hosts. Host count: [{}]", hostsMap.size());
        return hostsMap;
    }

    private void redistributeParcelsForRecovery() throws ApiException, CloudbreakException {
        LOGGER.debug("Refreshing parcel repos");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiCommand refreshParcelRepos = clouderaManagerResourceApi.refreshParcelRepos();
        ExtendedPollingResult activateParcelsPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient,
                refreshParcelRepos.getId());
        handlePollingResult(activateParcelsPollingResult.getPollingResult(), "Cluster was terminated while waiting for parcel repository refresh",
                "Timeout while Cloudera Manager was refreshing parcel repositories.");
        List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId());
        ExtendedPollingResult downloadPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient,
                refreshParcelRepos.getId(), products);
        handlePollingResult(downloadPollingResult, "Cluster was terminated while waiting for parcel download",
                "Timeout while Cloudera Manager was downloading parcels.");
        LOGGER.debug("Refreshed parcel repos");
    }

    public void restartStaleServices(ClustersResourceApi clustersResourceApi, boolean forced) throws ApiException, CloudbreakException {
        restartMgmtServices();
        deployConfigAndRefreshCMStaleServices(clustersResourceApi, forced);
    }

    private void restartMgmtServices() throws ApiException, CloudbreakException {
        MgmtServiceResourceApi mgmtServiceResourceApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient);
        restartClouderaManagementServices(mgmtServiceResourceApi);
    }

    @VisibleForTesting
    void deployConfigAndRefreshCMStaleServices(ClustersResourceApi clustersResourceApi, boolean forced) throws ApiException, CloudbreakException {
        LOGGER.debug("Redeploying client configurations and refreshing stale services in Cloudera Manager.");
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        List<ApiService> services = servicesResourceApi.readServices(stack.getName(), SUMMARY).getItems();
        List<ApiService> notFreshServices = getNotFreshServices(services, ApiService::getConfigStalenessStatus);
        List<ApiService> notFreshClientServices = getNotFreshServices(services, ApiService::getClientConfigStalenessStatus);
        if (!notFreshServices.isEmpty() || !notFreshClientServices.isEmpty()) {
            deployConfigAndRefreshStaleServices(clustersResourceApi, notFreshServices, notFreshClientServices, forced);
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
            List<ApiService> notFreshClientServices, boolean forced) throws ApiException, CloudbreakException {
        LOGGER.debug("Services with config staleness status: {}", notFreshServices.stream()
                .map(it -> it.getName() + ": " + it.getConfigStalenessStatus())
                .collect(Collectors.joining(", ")));
        LOGGER.debug("Services with client config staleness status: {}", notFreshClientServices.stream()
                .map(it -> it.getName() + ": " + it.getClientConfigStalenessStatus())
                .collect(Collectors.joining(", ")));
        List<ApiCommand> commands = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY, null).getItems();
        deployClientConfig(clustersResourceApi, stack, commands);
        refreshServices(clustersResourceApi, forced, commands);
        LOGGER.debug("Config deployed and stale services are refreshed in Cloudera Manager.");
    }

    private void refreshServices(ClustersResourceApi clustersResourceApi, boolean forced, List<ApiCommand> commands) throws ApiException, CloudbreakException {
        try {
            ApiCommand refreshServicesCmd = clouderaManagerCommonCommandService.getApiCommand(
                    commands, "RefreshCluster", stack.getName(), clustersResourceApi::refresh);
            pollRefresh(refreshServicesCmd);
        } catch (Exception e) {
            LOGGER.error("Error occured during refreshing stale services, forced: {}", forced, e);
            if (!forced) {
                throw e;
            }
        }
    }

    private void deployConfig() throws ApiException, CloudbreakException {
        LOGGER.debug("Deploying client configs and refreshing services in Cloudera Manager.");
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_UPDATING_REMOTE_DATA_CONTEXT);
        ApiCommand deployCommand = clustersResourceApi.deployClientConfig(stack.getName());
        pollRefresh(deployCommand);
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_UPDATED_REMOTE_DATA_CONTEXT);
        LOGGER.debug("Deployed client configs and refreshed services in Cloudera Manager.");
    }

    private void restartServices(boolean rollingRestartEnabled) throws ApiException, CloudbreakException {
        clouderaManagerRestartService.doRestartServicesIfNeeded(apiClient, stack, rollingRestartEnabled);
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
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(
                stack, apiClient, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult.getPollingResult(), "Cluster was terminated while waiting for service restart",
                "Timeout while Cloudera Manager was restarting services.");
    }

    @VisibleForTesting
    void pollDeployConfig(BigDecimal commandId) throws CloudbreakException {
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(
                stack, apiClient, commandId);
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for config deploy",
                "Timeout while Cloudera Manager was config deploying services.");
    }

    @VisibleForTesting
    void pollRefresh(ApiCommand restartCommand) throws CloudbreakException {
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(
                stack, apiClient, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for service refresh",
                "Timeout while Cloudera Manager was refreshing services.");
    }

    private void applyHostGroupRolesOnUpscaledHosts(ApiHostRefList body, String hostGroupName) throws ApiException, CloudbreakException {
        LOGGER.debug("Applying host template on upscaled hosts. Host group: [{}]", hostGroupName);
        HostTemplatesResourceApi templatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(apiClient);
        ApiCommand applyHostTemplateCommand = templatesResourceApi.applyHostTemplate(stack.getName(), hostGroupName, START_ROLES_ON_UPSCALED_NODES, body);
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(
                stack, apiClient, applyHostTemplateCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for host template to apply",
                "Timeout while Cloudera Manager was applying host template.");
        LOGGER.debug("Applied host template on upscaled hosts. Host group: [{}]", hostGroupName);
    }

    private void activateParcels(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Deploying client configurations on upscaled hosts.");
        BigDecimal deployCommandId = deployClientConfig(clustersResourceApi, stack);
        List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId());
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClient, deployCommandId, products);
        handlePollingResult(pollingResult, "Cluster was terminated while waiting for parcels activation",
                "Timeout while Cloudera Manager activate parcels.");
        LOGGER.debug("Parcels are activated on upscaled hosts.");
    }

    private ApiHostRefList createUpscaledHostRefList(Map<String, InstanceMetaData> upscaleInstancesMap, Map<String, ApiHost> upscaleHostsMap) {
        LOGGER.debug("Creating ApiHostRefList from upscaled hosts.");
        ApiHostRefList body = new ApiHostRefList();
        upscaleHostsMap.forEach((hostname, host) ->
                Optional.ofNullable(upscaleInstancesMap.get(hostname))
                        .ifPresent(instance -> {
                            ApiHostRef apiHostRef = new ApiHostRef()
                                    .hostname(instance.getDiscoveryFQDN())
                                    .hostId(host.getHostId());
                            body.addItemsItem(apiHostRef);
                        }));
        if (body.getItems() != null) {
            LOGGER.debug("Created ApiHostRefList from upscaled hosts. Host count: [{}]", body.getItems().size());
        } else {
            LOGGER.debug("Created ApiHostRefList is empty.");
        }
        return body;
    }

    private void setHostRackIdForOutdatedClusterHosts(Collection<InstanceMetaData> instanceMetaDatas, Set<String> clusterHostnames, List<ApiHost> hosts)
            throws ApiException {
        Map<String, InstanceMetaData> clusterInstancesMap = getInstancesMap(clusterHostnames, instanceMetaDatas, false);
        Map<String, ApiHost> clusterHostsMap = getHostsMap(clusterInstancesMap, hosts);
        setHostRackIdBatch(clusterInstancesMap, clusterHostsMap);
    }

    private void setHostRackIdBatch(Map<String, InstanceMetaData> instancesMap, Map<String, ApiHost> hostsMap) throws ApiException {
        LOGGER.debug("Setting rack ID for hosts with batch operation.");
        List<ApiBatchRequestElement> batchRequestElements = hostsMap.entrySet().stream()
                .map(entry -> setRackIdForHostIfExists(instancesMap, entry.getKey(), entry.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::getBatchRequestElementForUpdateHost)
                .collect(Collectors.toList());
        if (!batchRequestElements.isEmpty()) {
            updateHostsWithRackIdUsingBatchCall(batchRequestElements);
        } else {
            LOGGER.debug("Setting rack ID for hosts batch operation canceled, there is nothing to update.");
        }
    }

    private Optional<ApiHost> setRackIdForHostIfExists(Map<String, InstanceMetaData> instancesMap, String hostname, ApiHost host) {
        return getRackId(instancesMap, hostname, host).map(host::rackId);
    }

    private Optional<String> getRackId(Map<String, InstanceMetaData> instancesMap, String hostname, ApiHost host) {
        return Optional.ofNullable(instancesMap.get(hostname))
                .flatMap(instance -> Optional.ofNullable(instance.getRackId()))
                .filter(Predicate.not(String::isEmpty))
                .filter(rackId -> !rackId.equals(host.getRackId()));
    }

    private ApiBatchRequestElement getBatchRequestElementForUpdateHost(ApiHost host) {
        // This has the same effect as com.cloudera.api.swagger.HostsResourceApi.updateHost(String hostId, com.cloudera.api.swagger.model.ApiHost)
        return new ApiBatchRequestElement()
                .method(HTTPMethod.PUT)
                .url(ClouderaManagerApiClientProvider.API_V_31 + "/hosts/" + URLUtils.encodeString(host.getHostId()))
                .body(host)
                .acceptType("application/json")
                .contentType("application/json");
    }

    private void updateHostsWithRackIdUsingBatchCall(List<ApiBatchRequestElement> batchRequestElements) throws ApiException {
        BatchResourceApi batchResourceApi = clouderaManagerApiFactory.getBatchResourceApi(apiClient);
        ApiBatchRequest batchRequest = new ApiBatchRequest().items(batchRequestElements);
        ApiBatchResponse batchResponse = batchResourceApi.execute(batchRequest);
        validateBatchResponse(batchResponse);
    }

    private void validateBatchResponse(ApiBatchResponse batchResponse) {
        if (batchResponse != null && batchResponse.getSuccess() != null && batchResponse.getItems() != null && batchResponse.getSuccess()) {
            // batchResponse contains the updated ApiHost for each request as well, but we are going to ignore them here
            LOGGER.debug("Setting rack ID for hosts batch operation finished. Updated host count: [{}].", batchResponse.getItems().size());
        } else {
            throw new ClouderaManagerOperationFailedException("Setting rack ID for hosts batch operation failed. Response: " + batchResponse);
        }
    }

    @Override
    public void stopCluster(boolean disableKnoxAutorestart) throws CloudbreakException {
        ClusterView cluster = stack.getCluster();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            LOGGER.debug("Stopping all Cloudera Runtime services");
            ExtendedPollingResult extendedPollingResult = clouderaManagerPollingServiceProvider.checkCmStatus(stack, apiClient);
            if (extendedPollingResult.isSuccess()) {
                stopWithRunningCm(disableKnoxAutorestart, cluster, clustersResourceApi);
            } else {
                logPollingResult(extendedPollingResult);
                skipStopWithStoppedCm();
            }
        } catch (ApiException e) {
            LOGGER.info("Couldn't stop Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void logPollingResult(ExtendedPollingResult extendedPollingResult) {
        String errorMessage = "Unknown error from poller";
        if (extendedPollingResult.getException() != null) {
            errorMessage = extendedPollingResult.getException().getMessage();
        }
        LOGGER.info("We will skip the CM stop, because the polling result wasn't success: {}, error: {}", extendedPollingResult.getPollingResult(),
                errorMessage, extendedPollingResult.getException());
    }

    private void skipStopWithStoppedCm() {
        LOGGER.debug("No need to stop Cloudera Manager services as Cloudera Manager is already stopped");
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
    }

    private void stopWithRunningCm(boolean disableKnoxAutorestart, ClusterView cluster, ClustersResourceApi clustersResourceApi)
            throws ApiException, CloudbreakException {
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
        disableKnoxAutorestart(disableKnoxAutorestart);
        Collection<ApiService> apiServices = readServices(stack);
        boolean anyServiceNotStopped = apiServices.stream()
                .anyMatch(service -> !ApiServiceState.STOPPED.equals(service.getServiceState())
                        && !ApiServiceState.STOPPING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()));
        if (anyServiceNotStopped) {
            ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, apiClient, apiCommand.getId());
            handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for Hadoop services to stop",
                    "Timeout while stopping Cloudera Manager services.");
        }
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
    }

    private void disableKnoxAutorestart(boolean disableKnoxAutorestart) {
        if (disableKnoxAutorestart) {
            configService.disableKnoxAutorestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, apiClient, stack.getName());
        }
    }

    private Collection<ApiService> readServices(StackDtoDelegate stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        return api.readServices(stack.getName(), SUMMARY).getItems();
    }

    @Override
    public void startClusterMgmtServices() throws CloudbreakException {
        startClouderaManager();
        startAgents();
    }

    /**
     * Deploy the client configuration and then start the cluster services.
     */
    @Override
    public void deployConfigAndStartClusterServices() throws CloudbreakException {
        try {
            LOGGER.info("Deploying configuration and restarting services");
            enableKnoxAutoRestart();
            deployConfig();
            restartServices(false);
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void startCluster() throws CloudbreakException {
        try {
            startClouderaManager();
            startAgents();
            enableKnoxAutoRestart();
            startServices();
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
    public Set<ParcelInfo> gatherInstalledParcels(String stackName) {
        return clouderaManagerParcelManagementService.getParcelsInStatus(clouderaManagerApiFactory.getParcelsResourceApi(apiClient), stackName, ACTIVATED);
    }

    @Override
    public Set<ParcelInfo> getAllParcels(String stackName) {
        try {
            ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(apiClient);
            Set<ParcelInfo> availableParcels = clouderaManagerParcelManagementService.getAllParcels(parcelsResourceApi, stackName);
            LOGGER.debug("The following parcels are available on the CM server: {}", availableParcels);
            return availableParcels;
        } catch (ApiException e) {
            LOGGER.error("Error during retrieving parcels!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public ParcelOperationStatus removeUnusedParcels(Set<ClusterComponentView> usedParcelComponents, Set<String> parcelNamesFromImage) {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(apiClient);
        ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(apiClient);
        Set<String> usedParcelComponentNames = usedParcelComponents.stream()
                .map(component -> component.getAttributes().getSilent(ClouderaManagerProduct.class).getName())
                .collect(Collectors.toSet());
        ParcelOperationStatus deactivateStatus = clouderaManagerParcelDecommissionService
                .deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(), usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus undistributeStatus = clouderaManagerParcelDecommissionService
                .undistributeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus removalStatus = clouderaManagerParcelDecommissionService
                .removeUnusedParcels(apiClient, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus result = removalStatus.merge(deactivateStatus).merge(undistributeStatus);
        LOGGER.info("Result of the parcel removal: {}", result);
        return result;
    }

    private void startServices() throws ApiException, CloudbreakException {
        ClusterView cluster = stack.getCluster();
        ClustersResourceApi apiInstance = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        String clusterName = cluster.getName();
        LOGGER.debug("Starting all services for cluster.");
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTING);
        Collection<ApiService> apiServices = readServices(stack);
        Set<ApiService> notStartedServices = apiServices.stream()
                .filter(service -> !ApiServiceState.STARTED.equals(service.getServiceState())
                        && !ApiServiceState.STARTING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()))
                .collect(Collectors.toSet());
        if (!notStartedServices.isEmpty()) {
            LOGGER.debug("Starting cluster because the following services are not running: {}", notStartedServices.stream()
                    .map(ApiService::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet()));
            ApiCommand apiCommand = apiInstance.startCommand(clusterName);
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient, apiCommand.getId());
            handlePollingResult(pollingResult, "Cluster was terminated while waiting for Cloudera Runtime services to start",
                    "Timeout while stopping Cloudera Manager services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTED);
    }

    private void startAgents() {
        LOGGER.debug("Starting Cloudera Manager agents on the hosts.");
        ExtendedPollingResult hostsJoinedResult = clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, apiClient);
        if (hostsJoinedResult.isExited()) {
            throw new CancellationException("Cluster was terminated while starting Cloudera Manager agents.");
        }
    }

    private void startClouderaManager() throws CloudbreakException {
        LOGGER.debug("Starting Cloudera Manager server on the hosts.");
        ExtendedPollingResult healthCheckResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient);
        handlePollingResult(healthCheckResult, "Cluster was terminated while waiting for Cloudera Manager to start.",
                "Cloudera Manager server was not restarted properly.");
    }

    @Override
    public void restartAll(boolean withMgmtServices) {
        try {
            restartServices(false);
            if (withMgmtServices) {
                restartClouderaManagementServices(clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient));
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Could not restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void restartClusterServices() {
        try {
            restartServices(false);
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Could not restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void handlePollingResult(PollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        pollingResultErrorHandler.handlePollingResult(pollingResult, cancellationMessage, timeoutMessage);
    }

    private void handlePollingResult(ExtendedPollingResult pollingResult, String cancellationMessage, String timeoutMessage) throws CloudbreakException {
        pollingResultErrorHandler.handlePollingResult(pollingResult, cancellationMessage, timeoutMessage);
    }

    @Override
    public void updateServiceConfigAndRestartService(String serviceType, String configName, String newConfigValue) throws Exception {
        configService.modifyServiceConfig(apiClient, stack.getName(), serviceType, Collections.singletonMap(configName, newConfigValue));
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        restartStaleServices(clustersResourceApi, false);
    }

    @Override
    public void updateServiceConfig(String serviceName, Map<String, String> config) throws CloudbreakException {
        configService.modifyServiceConfig(apiClient, stack.getName(), serviceName, config);
    }

    @Override
    public Optional<String> getRoleConfigValueByServiceType(String clusterName, String roleConfigGroup, String serviceType, String configName) {
        return configService.getRoleConfigValueByServiceType(apiClient, clusterName, roleConfigGroup, serviceType, configName);
    }

    @Override
    public boolean isRolePresent(String clusterName, String roleConfigGroup, String serviceType) {
        return configService.isRolePresent(apiClient, clusterName, roleConfigGroup, serviceType);
    }

    @Override
    public boolean isServicePresent(String clusterName, String serviceType) {
        boolean servicePresent = false;
        try {
            servicePresent = readServices(stack)
                    .stream()
                    .anyMatch(service -> serviceType.equalsIgnoreCase(service.getType()));
        } catch (ApiException e) {
            LOGGER.debug("Failed to determine if {} service is present in cluster {}.", serviceType, stack.getCluster().getId());
        }
        return servicePresent;
    }
}
