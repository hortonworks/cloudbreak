package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_10_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_5_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_0;
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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.AllHostsResourceApi;
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
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiEntityTag;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.config.ClouderaManagerFlinkConfigurationService;
import com.sequenceiq.cloudbreak.cm.config.modification.ClouderaManagerConfigModificationService;
import com.sequenceiq.cloudbreak.cm.config.modification.CmConfig;
import com.sequenceiq.cloudbreak.cm.config.modification.CmServiceType;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerParcelActivationTimeoutException;
import com.sequenceiq.cloudbreak.cm.model.ClouderaManagerClientConfigDeployRequest;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.service.ScalingException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.URLUtils;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerModificationService implements ClusterModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerModificationService.class);

    private static final Boolean START_ROLES_ON_UPSCALED_NODES = Boolean.TRUE;

    private static final String HOST_TEMPLATE_NAME_TAG = "_cldr_cm_host_template_name";

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private CloudbreakEventService eventService;

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

    @Inject
    private ClouderaManagerConfigModificationService clouderaManagerConfigModificationService;

    @Inject
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    @Inject
    private ClusterCommandService clusterCommandService;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Inject
    private ClouderaManagerFlinkConfigurationService clouderaManagerFlinkConfigurationService;

    @Inject
    private ClouderaManagerClientConfigDeployService clouderaManagerClientConfigDeployService;

    private ApiClient v31Client;

    private ApiClient v52Client;

    public ClouderaManagerModificationService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            v31Client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
        try {
            v52Client = clouderaManagerApiClientProvider.getV52Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            LOGGER.warn("Client init failed for V52 client!");
        }
    }

    @Override
    public List<String> upscaleCluster(Map<HostGroup, Set<InstanceMetaData>> instanceMetaDatasByHostGroup) throws CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(v31Client);
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
                Boolean prewarmed = clusterComponentProvider.getClouderaManagerRepoDetails(stack.getCluster().getId()).getPredefined();
                if (Boolean.FALSE.equals(prewarmed)) {
                    redistributeParcelsForRecovery();
                }
                activateParcels(clustersResourceApi);
            }
            clouderaManagerClientConfigDeployService.deployAndPollClientConfig(
                    ClouderaManagerClientConfigDeployRequest.builder()
                            .pollerMessage("Deploy client configurations from upscaleCluster")
                            .clustersResourceApi(clustersResourceApi)
                            .client(v31Client)
                            .stack(stack)
                            .build()
            );
            clouderaManagerRoleRefreshService.refreshClusterRoles(v31Client, stack);
            LOGGER.debug("Cluster upscale completed.");
            return instanceMetaDatas.stream()
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            LOGGER.error(String.format("Failed to upscale. Response: %s", e.getResponseBody()), e);
            throwScalingException(upscaleInstancesMap, e);
        } catch (ClouderaManagerParcelActivationTimeoutException e) {
            LOGGER.error(String.format("Failed to upscale. Response: %s", e.getMessage()), e);
            throwScalingException(upscaleInstancesMap, e);
        }
        return Collections.emptyList();
    }

    private void throwScalingException(Map<String, InstanceMetaData> upscaleInstancesMap, Exception e) throws ScalingException {
        throw new ScalingException("Failed to upscale", e,
                upscaleInstancesMap.values().stream().map(InstanceMetaData::getInstanceId).collect(Collectors.toSet()));
    }

    private ApiHostRefList getBodyForApplyHostGroupRoles(Map<String, InstanceMetaData> upscaleInstancesMap, Map<String, ApiHost> upscaleHostsMap,
            HostGroup hostGroup) {
        Map<String, InstanceMetaData> upscaleInstancesMapForHostGroup =
                upscaleInstancesMap.entrySet().stream()
                        .filter(instanceEntry -> hostGroup.getName().equals(instanceEntry.getValue().getInstanceGroupName()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return createUpscaledHostRefList(upscaleInstancesMapForHostGroup, upscaleHostsMap);
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
    public void upgradeClusterRuntime(Set<ClouderaManagerProduct> products, boolean patchUpgrade, Optional<String> remoteDataContext,
            boolean rollingUpgradeEnabled) throws CloudbreakException {
        try {
            LOGGER.info("Starting to upgrade cluster runtimes. Patch upgrade: {}", patchUpgrade);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(v31Client);
            ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);

            startClusterManagerAndAgents();
            tagHostsWithHostTemplateName();
            disableKnoxAutorestart(rollingUpgradeEnabled);
            refreshRemoteDataContextFromDatalakeInCaseOfDatahub(remoteDataContext);
            updateParcelSettings(products);
            restartMgmtServices();
            addFlinkServiceConfigurationIfNecessary(products);
            downloadParcels(products, parcelResourceApi, parcelsResourceApi);
            distributeParcels(products, parcelResourceApi, parcelsResourceApi);
            LOGGER.debug("Starting the upgrade for the new components: {}", products);
            if (patchUpgrade) {
                activateParcels(products, parcelResourceApi, parcelsResourceApi);
                clouderaManagerRestartService.waitForRestartExecutionIfPresent(v31Client, stack, rollingUpgradeEnabled);
                startServices();
                callPostClouderaRuntimeUpgradeCommandIfCMIsNewerThan751(rollingUpgradeEnabled);
                restartServices(rollingUpgradeEnabled, false);
            } else {
                ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(v31Client);
                upgradeNonCdhProducts(products, parcelResourceApi, parcelsResourceApi);
                upgradeCdh(clustersResourceApi, products, rollingUpgradeEnabled);
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

    private void addFlinkServiceConfigurationIfNecessary(Set<ClouderaManagerProduct> products) {
        clouderaManagerFlinkConfigurationService.addServiceConfigurationIfNecessary(v31Client, stack, products);
    }

    private void enableKnoxAutoRestart() {
        configService.modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), true);
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
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
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
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(v31Client);
            clouderaManagerParcelManagementService.checkParcelApiAvailability(stack, v31Client);
            clouderaManagerParcelManagementService.setParcelRepos(products, clouderaManagerResourceApi);
            clouderaManagerParcelManagementService.refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        } catch (ApiException e) {
            LOGGER.error("Error during updating parcel settings!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void downloadParcels(Set<ClouderaManagerProduct> products) throws CloudbreakException {
        try {
            LOGGER.debug("Downloading parcels: {}", products);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(v31Client);
            ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);
            downloadParcels(products, parcelResourceApi, parcelsResourceApi);
        } catch (ApiException e) {
            LOGGER.error("Error during downloading parcels!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void distributeParcels(Set<ClouderaManagerProduct> products) throws CloudbreakException {
        try {
            LOGGER.debug("Distributing parcels: {}", products);
            ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(v31Client);
            ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);
            distributeParcels(products, parcelResourceApi, parcelsResourceApi);
        } catch (ApiException e) {
            LOGGER.error("Error during distributing parcels!", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void upgradeCdh(ClustersResourceApi clustersResourceApi, Set<ClouderaManagerProduct> products, boolean rollingUpgradeEnabled)
            throws ApiException, CloudbreakException {
        Optional<ClouderaManagerProduct> cdhProduct = clouderaManagerProductsProvider.getCdhProduct(products);
        if (cdhProduct.isEmpty()) {
            LOGGER.debug("Skipping CDH product upgrade because upgrade candidate list not contains any CDH parcel.");
        } else {
            callUpgradeCdhCommand(cdhProduct.get(), clustersResourceApi, rollingUpgradeEnabled);
        }
    }

    private void upgradeNonCdhProducts(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi)
            throws CloudbreakException, ApiException {
        Set<ClouderaManagerProduct> nonCdhServices = clouderaManagerProductsProvider.getNonCdhProducts(products);
        if (!nonCdhServices.isEmpty()) {
            List<String> productNames = nonCdhServices.stream().map(ClouderaManagerProduct::getName).collect(Collectors.toList());
            LOGGER.debug("Starting to upgrade the following Non-CDH products: {}", productNames);
            activateParcels(nonCdhServices, parcelResourceApi, parcelsResourceApi);
        } else {
            LOGGER.debug("Skipping Non-CDH products upgrade because the cluster does not contains any other products beside CDH.");
        }
    }

    private void callUpgradeCdhCommand(ClouderaManagerProduct cdhProduct, ClustersResourceApi clustersResourceApi, boolean rollingUpgradeEnabled)
            throws ApiException, CloudbreakException {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                rollingUpgradeEnabled ? ResourceEvent.CLUSTER_UPGRADE_START_ROLLING_UPGRADE : ResourceEvent.CLUSTER_UPGRADE_START_UPGRADE);
        clouderaManagerUpgradeService.callUpgradeCdhCommand(cdhProduct.getVersion(), clustersResourceApi, stack, v31Client, rollingUpgradeEnabled);
    }

    private void callPostClouderaRuntimeUpgradeCommandIfCMIsNewerThan751(boolean rollingUpgradeEnabled) throws ApiException, CloudbreakException {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        String currentCMVersion = clouderaManagerRepo.getVersion();
        Versioned baseCMVersion = CLOUDERAMANAGER_VERSION_7_5_1;
        if (isVersionNewerOrEqualThanLimited(currentCMVersion, baseCMVersion)) {
            LOGGER.debug("Cloudera Manager version {} is newer than {} hence calling post runtime upgrade command using /v45 API",
                    currentCMVersion, baseCMVersion.getVersion());
            eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
            clouderaManagerRestartService.waitForRestartExecutionIfPresent(v31Client, stack, rollingUpgradeEnabled);
            ClusterView cluster = stack.getCluster();
            String user = cluster.getCloudbreakClusterManagerUser();
            String password = cluster.getCloudbreakClusterManagerPassword();
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

    private void distributeParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi)
            throws ApiException, CloudbreakException {
        clouderaManagerParcelManagementService.distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
    }

    private void activateParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi)
            throws ApiException, CloudbreakException {
        clouderaManagerParcelManagementService.activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
    }

    private void downloadParcels(Set<ClouderaManagerProduct> products, ParcelResourceApi parcelResourceApi, ParcelsResourceApi parcelsResourceApi)
            throws ApiException, CloudbreakException {
        clouderaManagerParcelManagementService.downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
    }

    private void removeUnusedParcelVersions(ParcelResourceApi parcelResourceApi, Set<ClouderaManagerProduct> products) throws ApiException {
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);
        for (ClouderaManagerProduct product : products) {
            LOGGER.info("Removing unused {} parcels.", product.getName());
            clouderaManagerParcelDecommissionService.removeUnusedParcelVersions(v31Client, parcelsResourceApi, parcelResourceApi, stack, product);
        }
    }

    private List<ApiHost> getHostsFromCM() throws ApiException {
        LOGGER.debug("Retrieving registered host details from CM.");
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(v31Client);
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
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(v31Client);
        ApiCommand refreshParcelRepos = clouderaManagerResourceApi.refreshParcelRepos();
        ExtendedPollingResult activateParcelsPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, v31Client,
                refreshParcelRepos.getId());
        handlePollingResult(activateParcelsPollingResult.getPollingResult(), "Cluster was terminated while waiting for parcel repository refresh",
                "Timeout while Cloudera Manager was refreshing parcel repositories.");
        List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId());
        ExtendedPollingResult downloadPollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, v31Client,
                refreshParcelRepos.getId(), products);
        handlePollingResult(downloadPollingResult, "Cluster was terminated while waiting for parcel download",
                "Timeout while Cloudera Manager was downloading parcels.");
        LOGGER.debug("Refreshed parcel repos");
    }

    public void restartStaleServices(ClustersResourceApi clustersResourceApi, boolean forced) throws ApiException, CloudbreakException {
        restartMgmtServices();
        deployConfigAndRefreshCMStaleServices(clustersResourceApi, forced);
    }

    @Override
    public void restartMgmtServices() throws ApiException, CloudbreakException {
        MgmtServiceResourceApi mgmtServiceResourceApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(v31Client);
        restartClouderaManagementServices(mgmtServiceResourceApi);
    }

    @VisibleForTesting
    void deployConfigAndRefreshCMStaleServices(ClustersResourceApi clustersResourceApi, boolean forced) throws ApiException, CloudbreakException {
        LOGGER.debug("Redeploying client configurations and refreshing stale services in Cloudera Manager.");
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(v31Client);
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
        clouderaManagerClientConfigDeployService.deployAndPollClientConfig(
                ClouderaManagerClientConfigDeployRequest.builder()
                        .pollerMessage("Deploy client configurations from deployConfigAndRefreshStaleServices")
                        .clustersResourceApi(clustersResourceApi)
                        .client(v31Client)
                        .stack(stack)
                        .build()
        );
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
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_UPDATING_REMOTE_DATA_CONTEXT);
        clouderaManagerClientConfigDeployService.deployAndPollClientConfig(
                ClouderaManagerClientConfigDeployRequest.builder()
                        .pollerMessage("Refresh cluster from deployConfig")
                        .clustersResourceApi(clouderaManagerApiFactory.getClustersResourceApi(v31Client))
                        .client(v31Client)
                        .stack(stack)
                        .build()
        );
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_UPDATED_REMOTE_DATA_CONTEXT);
        LOGGER.debug("Deployed client configs and refreshed services in Cloudera Manager.");
    }

    private void restartServices(boolean rollingRestartEnabled, boolean restartStaleOnly) throws ApiException, CloudbreakException {
        clouderaManagerRestartService.doRestartServicesIfNeeded(v31Client, stack, rollingRestartEnabled, restartStaleOnly,
                Optional.empty());
    }

    private void restartGivenServices(List<String> serviceNames) throws ApiException, CloudbreakException {
        clouderaManagerRestartService.doRestartServicesIfNeeded(v31Client, stack, false, false,
                Optional.ofNullable(serviceNames));
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
                stack, v31Client, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult.getPollingResult(), "Cluster was terminated while waiting for service restart",
                "Timeout while Cloudera Manager was restarting services.");
    }

    @VisibleForTesting
    void pollDeployConfig(BigDecimal commandId) throws CloudbreakException {
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(
                stack, v31Client, commandId);
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for config deploy",
                "Timeout while Cloudera Manager was config deploying services.");
    }

    @VisibleForTesting
    void pollRefresh(ApiCommand restartCommand) throws CloudbreakException {
        ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(
                stack, v31Client, restartCommand.getId());
        handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for service refresh",
                "Timeout while Cloudera Manager was refreshing services.");
    }

    private void applyHostGroupRolesOnUpscaledHosts(ApiHostRefList body, String hostGroupName) throws ApiException, CloudbreakException {
        if (body.getItems() != null && !body.getItems().isEmpty()) {
            LOGGER.debug("Applying host template on upscaled hosts. Host group: [{}]", hostGroupName);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
            ApiCommand applyHostTemplateCommand;
            if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_10_0)) {
                HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(v52Client);
                applyHostTemplateCommand = hostTemplatesResourceApi
                        .applyHostTemplate(stack.getName(), hostGroupName, true, START_ROLES_ON_UPSCALED_NODES, body);
            } else {
                HostTemplatesResourceApi hostTemplatesResourceApi = clouderaManagerApiFactory.getHostTemplatesResourceApi(v31Client);
                applyHostTemplateCommand = hostTemplatesResourceApi
                        .applyHostTemplate(stack.getName(), hostGroupName, false, START_ROLES_ON_UPSCALED_NODES, body);
            }
            ExtendedPollingResult hostTemplatePollingResult = clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(
                    stack, v31Client, applyHostTemplateCommand.getId());
            handlePollingResult(hostTemplatePollingResult, "Cluster was terminated while waiting for host template to apply",
                    "Timeout while Cloudera Manager was applying host template.");
            LOGGER.debug("Applied host template on upscaled hosts. Host group: [{}]", hostGroupName);
        } else {
            LOGGER.debug("Skip applying host template, empty host list for host group: {}", hostGroupName);
        }
    }

    private void activateParcels(ClustersResourceApi clustersResourceApi) throws ApiException, CloudbreakException {
        LOGGER.debug("Deploying client configurations on upscaled hosts.");
        BigDecimal deployCommandId = clouderaManagerClientConfigDeployService.deployClientConfig(
                ClouderaManagerClientConfigDeployRequest.builder()
                        .pollerMessage("Deploy client configurations from activateParcels")
                        .clustersResourceApi(clustersResourceApi)
                        .stack(stack)
                        .build()
        );
        List<ClouderaManagerProduct> products = clusterComponentConfigProvider.getClouderaManagerProductDetails(stack.getCluster().getId());
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, v31Client, deployCommandId, products);
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
        BatchResourceApi batchResourceApi = clouderaManagerApiFactory.getBatchResourceApi(v31Client);
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
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(v31Client);
        try {
            LOGGER.debug("Stopping all Cloudera Runtime services");
            ExtendedPollingResult extendedPollingResult = clouderaManagerPollingServiceProvider.checkCmStatus(stack, v31Client);
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
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
        disableKnoxAutorestart(disableKnoxAutorestart);
        Collection<ApiService> apiServices = readServices(stack);
        boolean anyServiceNotStopped = apiServices.stream()
                .anyMatch(service -> !ApiServiceState.STOPPED.equals(service.getServiceState())
                        && !ApiServiceState.STOPPING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()));
        if (anyServiceNotStopped) {
            ApiCommand apiCommand = clustersResourceApi.stopCommand(cluster.getName());
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, v31Client, apiCommand.getId());
            handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for Hadoop services to stop",
                    "Timeout while stopping Cloudera Manager services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
    }

    private void disableKnoxAutorestart(boolean disableKnoxAutorestart) {
        if (disableKnoxAutorestart) {
            configService.modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), false);
        }
    }

    private Collection<ApiService> readServices(StackDtoDelegate stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(v31Client);
        return api.readServices(stack.getName(), SUMMARY).getItems();
    }

    @Override
    public void startClusterManagerAndAgents() throws CloudbreakException {
        startClouderaManager();
        startAgents();
    }

    /**
     * Deploy the client configuration and then start the cluster services.
     */
    @Override
    public void deployConfigAndRestartClusterServices(boolean rollingRestart) throws CloudbreakException {
        try {
            LOGGER.info("Deploying configuration and restarting services");
            enableKnoxAutoRestart();
            deployConfig();
            restartServices(rollingRestart, false);
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void startCluster() throws CloudbreakException {
        startCluster(false);
    }

    @Override
    public void startCluster(boolean servicesOnly) throws CloudbreakException {
        try {
            if (!servicesOnly) {
                startClouderaManager();
                startAgents();
            }
            enableKnoxAutoRestart();
            startServices();
        } catch (ApiException e) {
            LOGGER.info("Couldn't start Cloudera Manager services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void reconfigureCMMemory() {
        try {
            LOGGER.info("configure CM memory settings.");
            configureCMMemoryThreasholdOnAllHosts();
        } catch (ApiException e) {
            LOGGER.warn("Couldn't Configure CM memory settings", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void configureCMMemoryThreasholdOnAllHosts() throws ApiException {
        AllHostsResourceApi allHostsResourceApi = clouderaManagerApiFactory.getAllHostsResourceApi(v31Client);
        ApiConfigList apiConfigList = new ApiConfigList();
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName("memory_overcommit_threshold");
        apiConfig.setValue("0.95");
        apiConfigList.addItemsItem(apiConfig);
        allHostsResourceApi.updateConfig(null, apiConfigList);
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
        return clouderaManagerParcelManagementService.getParcelsInStatus(clouderaManagerApiFactory.getParcelsResourceApi(v31Client), stackName, ACTIVATED);
    }

    @Override
    public Set<ParcelInfo> getAllParcels(String stackName) {
        try {
            ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);
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
        ParcelsResourceApi parcelsResourceApi = clouderaManagerApiFactory.getParcelsResourceApi(v31Client);
        ParcelResourceApi parcelResourceApi = clouderaManagerApiFactory.getParcelResourceApi(v31Client);
        Set<String> usedParcelComponentNames = usedParcelComponents.stream()
                .map(component -> component.getAttributes().getSilent(ClouderaManagerProduct.class).getName())
                .collect(Collectors.toSet());
        ParcelOperationStatus deactivateStatus = clouderaManagerParcelDecommissionService
                .deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(), usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus undistributeStatus = clouderaManagerParcelDecommissionService
                .undistributeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus removalStatus = clouderaManagerParcelDecommissionService
                .removeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames, parcelNamesFromImage);
        ParcelOperationStatus result = removalStatus.merge(deactivateStatus).merge(undistributeStatus);
        LOGGER.info("Result of the parcel removal: {}", result);
        return result;
    }

    private void startServices() throws ApiException, CloudbreakException {
        ClusterView cluster = stack.getCluster();
        ClustersResourceApi apiInstance = clouderaManagerApiFactory.getClustersResourceApi(v31Client);
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
            ClusterCommand startCommand = null;
            try {
                startCommand = startServicesIfNotRunning(cluster, apiInstance);
                ExtendedPollingResult pollingResult =
                        clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, startCommand.getCommandId());
                handlePollingResult(pollingResult, "Cluster was terminated while waiting for Cloudera Runtime services to start",
                        "Timeout while stopping Cloudera Manager services.");
            } finally {
                if (startCommand != null) {
                    clusterCommandService.delete(startCommand);
                }
            }
        }
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_STARTED);
    }

    private ClusterCommand startServicesIfNotRunning(ClusterView cluster, ClustersResourceApi clustersResourceApi)
            throws ApiException {
        Optional<ClusterCommand> startClusterCommand =
                clusterCommandService.findTopByClusterIdAndClusterCommandType(cluster.getId(), ClusterCommandType.START_CLUSTER);
        if (startClusterCommand.isPresent()) {
            Optional<ApiCommand> apiCommand = clouderaManagerCommandsService.getApiCommandIfExist(v31Client, startClusterCommand.get().getCommandId());
            if (apiCommand.isPresent() && Boolean.TRUE.equals(apiCommand.get().getActive())) {
                return startClusterCommand.get();
            } else {
                clusterCommandService.delete(startClusterCommand.get());
                return startServicesAndStoreCMCommand(cluster, clustersResourceApi);
            }
        } else {
            return startServicesAndStoreCMCommand(cluster, clustersResourceApi);
        }
    }

    private ClusterCommand startServicesAndStoreCMCommand(ClusterView cluster, ClustersResourceApi clustersResourceApi) throws ApiException {
        ApiCommand startCommand = clustersResourceApi.startCommand(stack.getName());
        ClusterCommand newStartClusterCommand = new ClusterCommand();
        newStartClusterCommand.setClusterId(cluster.getId());
        newStartClusterCommand.setCommandId(startCommand.getId());
        newStartClusterCommand.setClusterCommandType(ClusterCommandType.START_CLUSTER);
        return clusterCommandService.save(newStartClusterCommand);
    }

    private void startAgents() {
        LOGGER.debug("Starting Cloudera Manager agents on the hosts.");
        ExtendedPollingResult hostsJoinedResult = clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client);
        if (hostsJoinedResult.isExited()) {
            throw new CancellationException("Cluster was terminated while starting Cloudera Manager agents.");
        }
    }

    private void startClouderaManager() throws CloudbreakException {
        LOGGER.debug("Starting Cloudera Manager server on the hosts.");
        ExtendedPollingResult healthCheckResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client);
        handlePollingResult(healthCheckResult, "Cluster was terminated while waiting for Cloudera Manager to start.",
                "Cloudera Manager server was not restarted properly.");
    }

    @Override
    public void restartAll(boolean withMgmtServices) {
        try {
            restartServices(false, false);
            if (withMgmtServices) {
                restartClouderaManagementServices(clouderaManagerApiFactory.getMgmtServiceResourceApi(v31Client));
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Could not restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void restartServiceRoleByType(String serviceType, String roleType) {
        clouderaManagerRestartService.restartServiceRoleByType(stack, v31Client, serviceType, roleType);
    }

    @Override
    public void restartClusterServices() {
        try {
            restartServices(false, false);
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
        configService.modifyServiceConfig(v31Client, stack.getName(), serviceType, Collections.singletonMap(configName, newConfigValue));
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(v31Client);
        restartStaleServices(clustersResourceApi, false);
    }

    @Override
    public void updateServiceConfig(String serviceName, Map<String, String> config) throws CloudbreakException {
        configService.modifyServiceConfig(v31Client, stack.getName(), serviceName, config);
    }

    @Override
    public Optional<String> getRoleConfigValueByServiceType(String clusterName, String roleConfigGroup, String serviceType, String configName) {
        return configService.getRoleConfigValueByServiceType(v31Client, clusterName, roleConfigGroup, serviceType, configName);
    }

    @Override
    public boolean isRolePresent(String clusterName, String roleConfigGroup, String serviceType) {
        return configService.isRolePresent(v31Client, clusterName, roleConfigGroup, serviceType);
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

    @Override
    public void hostsStartRoles(List<String> hosts) {
        if (!hosts.isEmpty()) {
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
            LOGGER.info("CM version is: {}", clouderaManagerRepoDetails.getVersion());
            if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails.getVersion(), CLOUDERAMANAGER_VERSION_7_9_0)) {
                LOGGER.info("Current action is repair and CM version is newer than 7.9.0, start services on hosts because services were stopped");
                if (!hosts.isEmpty()) {
                    LOGGER.info("Start roles on hosts: {}", hosts);
                    clusterCommandService.findTopByClusterIdAndClusterCommandType(stack.getCluster().getId(), ClusterCommandType.HOST_START_ROLES)
                            .ifPresentOrElse(this::waitStartRolesCommand, () -> {
                                try {
                                    ApiHostNameList items = new ApiHostNameList().items(hosts);
                                    ApiCommand apiCommand = clouderaManagerApiFactory.getClouderaManagerResourceApi(v31Client).hostsStartRolesCommand(items);

                                    ClusterCommand clusterCommand = new ClusterCommand();
                                    clusterCommand.setClusterId(stack.getCluster().getId());
                                    clusterCommand.setCommandId(apiCommand.getId());
                                    clusterCommand.setClusterCommandType(ClusterCommandType.HOST_START_ROLES);
                                    clusterCommandService.save(clusterCommand);

                                    waitStartRolesCommand(clusterCommand);
                                } catch (ApiException e) {
                                    LOGGER.error("Failed to start roles on nodes: {}", hosts, e);
                                    throw new CloudbreakServiceException("Failed to start roles on nodes: " + hosts, e);
                                }
                            });

                }
            }
        } else {
            LOGGER.warn("Don't run start roles command because hosts are empty");
        }
    }

    private void waitStartRolesCommand(ClusterCommand clusterCommand) {
        ExtendedPollingResult extendedPollingResult =
                clouderaManagerPollingServiceProvider.startPollingStartRolesCommand(stack, v31Client, clusterCommand.getCommandId());
        clusterCommandService.delete(clusterCommand);
        if (extendedPollingResult.isExited()) {
            throw new CancellationException("Cluster was terminated while waiting for start roles on hosts");
        } else if (extendedPollingResult.isTimeout()) {
            throw new CloudbreakServiceException(
                    String.format("Cloudera Manager start roles command {} timed out. CM command Id: %s", clusterCommand.getCommandId()));
        }
    }

    @Override
    public void rollingRestartServices(boolean restartStaleOnly) {
        try {
            restartServices(true, restartStaleOnly);
        } catch (ClouderaManagerOperationFailedException e) {
            if (e.getMessage().contains("Command Rolling Restart is not currently available for execution")) {
                //https://cloudera.atlassian.net/browse/OPSAPS-70856
                tryWithoutRollingRestartCommand(e);
            } else {
                throw e;
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.warn("Could not perform rolling restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void tryWithoutRollingRestartCommand(ClouderaManagerOperationFailedException e) {
        try {
            restartServices(false, false);
        } catch (ApiException | CloudbreakException ex) {
            LOGGER.warn("Could not perform rolling restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void updateConfig(Table<String, String, String> configTable, CMConfigUpdateStrategy cmConfigUpdateStrategy) throws Exception {
        List<CmConfig> newConfigs = configTable.cellSet().stream()
                .map(cell -> new CmConfig(new CmServiceType(cell.getRowKey()), cell.getColumnKey(), cell.getValue()))
                .toList();
        clouderaManagerConfigModificationService.updateConfigs(newConfigs, v31Client, stack, cmConfigUpdateStrategy);
        LOGGER.info("Updating relevant configs finished for cluster {} in CM, deploying client configs and restarting services.", stack.getName());
        clouderaManagerRoleRefreshService.refreshClusterRoles(v31Client, stack);
        List<String> serviceNames = clouderaManagerConfigModificationService.getServiceNames(newConfigs, v31Client, stack);
        restartGivenServices(serviceNames);
    }

    @Override
    public void stopClouderaManagerService(String serviceType, boolean waitForExecution) {
        clouderaManagerServiceManagementService.stopClouderaManagerService(v31Client, stack, serviceType, waitForExecution);
    }

    @Override
    public void updateConfigWithoutRestart(Table<String, String, String> configTable, CMConfigUpdateStrategy cmConfigUpdateStrategy) throws Exception {
        List<CmConfig> newConfigs = configTable.cellSet().stream()
                .map(cell -> new CmConfig(new CmServiceType(cell.getRowKey()), cell.getColumnKey(), cell.getValue()))
                .toList();
        clouderaManagerConfigModificationService.updateConfigs(newConfigs, v31Client, stack, cmConfigUpdateStrategy);
        LOGGER.info("Updating relevant configs finished for cluster {} in CM, deploying client configs and restarting services is skipped.", stack.getName());
    }

    @Override
    public void deleteClouderaManagerService(String serviceType) {
        clouderaManagerServiceManagementService.deleteClouderaManagerService(v31Client, stack, serviceType);
    }

    @Override
    public void startClouderaManagerService(String serviceType, boolean waitForExecution) {
        clouderaManagerServiceManagementService.startClouderaManagerService(v31Client, stack, serviceType, waitForExecution);
    }

    @Override
    public Map<String, String> fetchServiceStatuses() {
        ApiServiceList serviceSummary = clouderaManagerServiceManagementService.readServices(v31Client, stack.getName());
        return serviceSummary.getItems().stream()
                .collect(Collectors.toMap(ApiService::getName, item -> item.getServiceState().getValue()));
    }

    @Override
    public void updateServiceConfig(String serviceType, Map<String, String> config, List<String> roleGroupNames) {
        configService.modifyRoleBasedConfig(v31Client, stack.getName(), serviceType, config, roleGroupNames);
    }

}