package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_0_2;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
@Scope("prototype")
public class ClouderaManagerSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSetupService.class);

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ClouderaHostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private CentralCmTemplateUpdater cmTemplateUpdater;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private ClouderaManagerLicenseService clouderaManagerLicenseService;

    @Inject
    private ClouderaManagerMgmtSetupService mgmtSetupService;

    @Inject
    private ClouderaManagerKerberosService kerberosService;

    @Inject
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Inject
    private ClouderaManagerMgmtLaunchService clouderaManagerMgmtLaunchService;

    @Inject
    private ClouderaManagerSupportSetupService clouderaManagerSupportSetupService;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient apiClient;

    public ClouderaManagerSetupService(Stack stack, HttpClientConfig clientConfig) {
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
    public void waitForServer() throws CloudbreakException, ClusterClientInitException {
        ApiClient client = null;
        try {
            client = clouderaManagerApiClientProvider.getDefaultClient(stack.getGatewayPort(), clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, client);
        if (isSuccess(pollingResult)) {
            LOGGER.debug("Cloudera Manager server has successfully started! Polling result: {}", pollingResult);
        } else if (isExited(pollingResult)) {
            throw new CancellationException("Polling of Cloudera Manager server start has been cancelled.");
        } else {
            LOGGER.debug("Could not start Cloudera Manager. polling result: {}", pollingResult);
            throw new CloudbreakException(String.format("Could not start Cloudera Manager. polling result: '%s'", pollingResult));
        }
    }

    @Override
    public String prepareTemplate(
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup,
        TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxCrn,
        KerberosConfig kerberosConfig) {
        Long clusterId = stack.getCluster().getId();
        try {
            Set<InstanceMetaData> instances = instanceMetaDataByHostGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            waitForHosts(instances);
            String sdxContextName = Optional.ofNullable(sdxContext).map(this::createDataContext).orElse(null);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
            ApiClusterTemplate apiClusterTemplate = getCmTemplate(templatePreparationObject, sdxContextName, instanceMetaDataByHostGroup,
                    clouderaManagerRepoDetails, clusterId);

            return getExtendedBlueprintText(apiClusterTemplate);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.info("Error while building the cluster. Message: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public Cluster buildCluster(
        Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup,
        TemplatePreparationObject templatePreparationObject,
        String sdxContext,
        String sdxStackCrn,
        Telemetry telemetry,
        KerberosConfig kerberosConfig,
        String template) {
        Cluster cluster = stack.getCluster();
        Long clusterId = cluster.getId();
        try {
            Set<InstanceMetaData> instances = instanceMetaDataByHostGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            waitForHosts(instances);
            clouderaManagerLicenseService.validateClouderaManagerLicense(stack.getCreator());
            String sdxContextName = Optional.ofNullable(sdxContext).map(this::createDataContext).orElse(null);

            configureCmMgmtServices(templatePreparationObject, sdxStackCrn, telemetry, sdxContextName);
            configureCmSupportTag(templatePreparationObject);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
            ApiClusterTemplate apiClusterTemplate = JsonUtil.readValue(template, ApiClusterTemplate.class);
            cluster.setExtendedBlueprintText(getExtendedBlueprintText(apiClusterTemplate));
            LOGGER.info("Generated Cloudera cluster template: {}", AnonymizerUtil.anonymize(template));
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);

            updateConfig(clouderaManagerResourceApi, stack.getType());
            boolean prewarmed = isPrewarmed(clusterId);
            if (prewarmed) {
                refreshParcelRepos(clouderaManagerResourceApi);
            }
            installCluster(cluster, apiClusterTemplate, clouderaManagerResourceApi, prewarmed);
            clouderaManagerMgmtLaunchService.startManagementServices(stack, apiClient);
            configureKerberos(kerberosConfig, clouderaManagerRepoDetails);
            clouderaManagerConfigService.setCdpEnvironmentIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_0_2, apiClient, clientConfig);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (ApiException e) {
            LOGGER.info("Error while building the cluster. Message: {}; Response: {}", e.getMessage(), e.getResponseBody(), e);
            String msg = extractMessage(e);
            throw new ClouderaManagerOperationFailedException(msg, e);
        } catch (Exception e) {
            LOGGER.info("Error while building the cluster. Message: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
        return cluster;
    }

    private void configureKerberos(KerberosConfig kerberosConfig, ClouderaManagerRepo clouderaManagerRepoDetails)
            throws ApiException, CloudbreakException {
        if (!CMRepositoryVersionUtil.isEnableKerberosSupportedViaBlueprint(clouderaManagerRepoDetails)) {
            kerberosService.configureKerberosViaApi(apiClient, clientConfig, stack, kerberosConfig);
        }
    }

    private ApiClusterTemplate getCmTemplate(TemplatePreparationObject templatePreparationObject, String sdxContextName,
            Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, ClouderaManagerRepo clouderaManagerRepoDetails, Long clusterId) {

        List<ClouderaManagerProduct> clouderaManagerProductDetails = clusterComponentProvider.getClouderaManagerProductDetails(clusterId);
        Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);
        return cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings, clouderaManagerRepoDetails,
                clouderaManagerProductDetails, sdxContextName);
    }

    private void configureCmMgmtServices(TemplatePreparationObject templatePreparationObject, String sdxCrn, Telemetry telemetry,
            String sdxContextName) throws ApiException {
        Optional<ApiHost> optionalCmHost = getCmHost(templatePreparationObject, apiClient);
        if (optionalCmHost.isPresent()) {
            ApiHost cmHost = optionalCmHost.get();
            ApiHostRef cmHostRef = new ApiHostRef();
            cmHostRef.setHostId(cmHost.getHostId());
            cmHostRef.setHostname(cmHost.getHostname());
            mgmtSetupService.setupMgmtServices(stack, apiClient, cmHostRef, templatePreparationObject.getRdsConfigs(), telemetry, sdxContextName, sdxCrn);
        } else {
            LOGGER.warn("Unable to determine Cloudera Manager host. Skipping management services installation.");
        }
    }

    private void configureCmSupportTag(TemplatePreparationObject templatePreparationObject) throws ApiException {
        Optional<ApiHost> optionalCmHost = getCmHost(templatePreparationObject, apiClient);
        if (optionalCmHost.isPresent()) {
            clouderaManagerSupportSetupService.prepareSupportRole(apiClient, templatePreparationObject);
        } else {
            LOGGER.warn("Unable to determine Cloudera Manager host. Skipping support tag setup.");
        }
    }

    private Optional<ApiHost> getCmHost(TemplatePreparationObject templatePreparationObject, ApiClient apiClient) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        return hostsResourceApi.readHosts(DataView.SUMMARY.name()).getItems().stream().filter(
                host -> host.getHostname().equals(templatePreparationObject.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN().get()))
                .findFirst();
    }

    private String createDataContext(String sdxContext) {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient rootClient = clouderaManagerApiClientProvider.getRootClient(stack.getGatewayPort(), user, password, clientConfig);
            CdpResourceApi cdpResourceApi = clouderaManagerApiFactory.getCdpResourceApi(rootClient);
            ApiRemoteDataContext apiRemoteDataContext = JsonUtil.readValue(sdxContext, ApiRemoteDataContext.class);
            LOGGER.debug("Posting remote context to workload. EndpointId: {}", apiRemoteDataContext.getEndPointId());
            return cdpResourceApi.postRemoteContext(apiRemoteDataContext).getEndPointId();
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Error while creating data context using: {}", sdxContext, e);
            throw new ClouderaManagerOperationFailedException(String.format("Error while creating data context: %s", e.getMessage()), e);
        } catch (IOException e) {
            LOGGER.info("Failed to parse SDX context to CM API object.", e);
            throw new ClouderaManagerOperationFailedException(String.format("Failed to parse SDX context to CM API object: %s", e.getMessage()), e);
        }
    }

    private void installCluster(Cluster cluster, ApiClusterTemplate apiClusterTemplate, ClouderaManagerResourceApi clouderaManagerResourceApi,
            boolean prewarmed) throws ApiException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        ApiCluster cmCluster = null;
        try {
            cmCluster = clustersResourceApi.readCluster(cluster.getName());
        } catch (ApiException apiException) {
            if (apiException.getCode() != HttpStatus.SC_NOT_FOUND) {
                throw apiException;
            }
        }
        Optional<ApiCommand> clusterInstallCommand = clouderaManagerResourceApi.listActiveCommands("SUMMARY").getItems()
                .stream().filter(cmd -> "ClusterTemplateImport".equals(cmd.getName())).findFirst();
        if (cmCluster == null) {
            try {
                // addRepositories - if true the parcels repositories in the cluster template
                // will be added.
                clusterInstallCommand = Optional.of(clouderaManagerResourceApi.importClusterTemplate(!prewarmed, apiClusterTemplate));
                LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");
            } catch (ApiException e) {
                String msg = "Cluster template install failed: " + extractMessage(e);
                throw new ClouderaManagerOperationFailedException(msg, e);
            }
        }
        clusterInstallCommand.ifPresent(cmd -> clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(stack, apiClient, cmd.getId()));
    }

    private ApiConfig removeRemoteParcelRepos() {
        return new ApiConfig().name("remote_parcel_repo_urls").value("");
    }

    private ApiConfig setHeader(com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType stackType) {
        return new ApiConfig().name("custom_header_color").value(StackType.DATALAKE.equals(stackType) ? "RED" : "BLUE");
    }

    private void updateConfig(ClouderaManagerResourceApi clouderaManagerResourceApi,
        com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType stackType) {
        try {
            ApiConfigList apiConfigList = new ApiConfigList()
                    .addItemsItem(removeRemoteParcelRepos())
                    .addItemsItem(setHeader(stackType));
            clouderaManagerResourceApi.updateConfig("Updated configurations.", apiConfigList);
        } catch (ApiException e) {
            LOGGER.info("Error while updating configurations. Message {}, throwable: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void refreshParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi) {
        try {
            ApiCommand apiCommand = clouderaManagerResourceApi.refreshParcelRepos();
            clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient, apiCommand.getId());
        } catch (ApiException e) {
            LOGGER.info("Unable to refresh parcel repo", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private ApiClusterTemplate convertStringJsonToTemplate(String json) {
        try {
            return JsonUtil.readValue(json, ApiClusterTemplate.class);
        } catch (IOException e) {
            LOGGER.info("Invalid Cloudera template json", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private String getExtendedBlueprintText(ApiClusterTemplate apiClusterTemplate) {
        return JsonUtil.writeValueAsStringSilent(apiClusterTemplate);
    }

    @Override
    public void waitForHosts(Set<InstanceMetaData> hostsInCluster) throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        ApiClient client;
        try {
            client = clouderaManagerApiClientProvider.getClient(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
        clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, client);
    }

    @Override
    public void waitForServices(int requestId) {

    }

    @Override
    public String getSdxContext() {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient rootClient = clouderaManagerApiClientProvider.getRootClient(stack.getGatewayPort(), user, password, clientConfig);
            CdpResourceApi cdpResourceApi = clouderaManagerApiFactory.getCdpResourceApi(rootClient);
            LOGGER.debug("Get remote context from SDX cluster: {}", stack.getName());
            ApiRemoteDataContext remoteDataContext = cdpResourceApi.getRemoteContextByCluster(stack.getName());
            return JsonUtil.writeValueAsString(remoteDataContext);
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Error while getting remote context of SDX cluster: {}", stack.getName(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        } catch (JsonProcessingException e) {
            LOGGER.info("Failed to serialize remote context.", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void setupProxy(ProxyConfig proxyConfig) {
        LOGGER.info("Setup proxy for CM");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiConfigList proxyConfigList = new ApiConfigList();
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_server").value(proxyConfig.getServerHost()));
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_port").value(String.valueOf(proxyConfig.getServerPort())));
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_protocol").value(proxyConfig.getProtocol().toUpperCase()));
        if (proxyConfig.getUserName() != null) {
            proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_user").value(proxyConfig.getUserName()));
        }
        if (proxyConfig.getPassword() != null) {
            proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_password").value(proxyConfig.getPassword()));
        }
        try {
            LOGGER.info("Update settings with: " + proxyConfigList);
            clouderaManagerResourceApi.updateConfig("Update proxy settings", proxyConfigList);
        } catch (ApiException e) {
            String failMessage = "Update proxy settings failed";
            LOGGER.error(failMessage, e);
            throw new ClouderaManagerOperationFailedException(failMessage, e);
        }
    }

    private Boolean isPrewarmed(Long clusterId) {
        return clusterComponentProvider.getClouderaManagerRepoDetails(clusterId).getPredefined();
    }

    private String extractMessage(ApiException apiException) {
        if (StringUtils.isEmpty(apiException.getResponseBody())) {
            return apiException.getMessage();
        }
        try {
            JsonNode tree = JsonUtil.readTree(apiException.getResponseBody());
            JsonNode message = tree.get("message");
            if (message != null && message.isTextual()) {
                String text = message.asText();
                if (StringUtils.isNotEmpty(text)) {
                    return text;
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to parse API response body as JSON", e);
        }
        return apiException.getResponseBody();
    }
}
