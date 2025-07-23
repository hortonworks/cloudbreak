package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

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
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiConfigPolicy;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
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
import com.sequenceiq.cloudbreak.cm.error.mapper.ClouderaManagerStorageErrorMapper;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
@Scope("prototype")
public class ClouderaManagerSetupService implements ClusterSetupService {

    public static final String POLICY_VERSION = "1.0";

    public static final String POLICY_DESCRIPTION = "Cloudera configured API Config Policy with TLS ciphers";

    public static final String POLICY_DESCRIPTION_GOV = "Cloudera configured API Config Policy with TLS ciphers and FISMA login banner";

    public static final String POLICY_NAME = "Policy with TLS ciphers";

    public static final String POLICY_NAME_GOV = "Policy with TLS ciphers and FISMA login banner";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSetupService.class);

    private static final String SDX_ENTERPRISE_DATALAKE_TEXT = "enterprise-datalake";

    private static final int BAD_REQUEST_ERROR_CODE = 400;

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
    private ClouderaManagerMgmtLaunchService clouderaManagerMgmtLaunchService;

    @Inject
    private ClouderaManagerSupportSetupService clouderaManagerSupportSetupService;

    @Inject
    private ClouderaManagerYarnSetupService clouderaManagerYarnSetupService;

    @Inject
    private ClusterCommandService clusterCommandService;

    @Inject
    private ClouderaManagerStorageErrorMapper clouderaManagerStorageErrorMapper;

    @Inject
    private ClouderaManagerCipherService clouderaManagerCipherService;

    @Inject
    private ClouderaManagerFedRAMPService clouderaManagerFedRAMPService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    private ApiClient apiClient;

    public ClouderaManagerSetupService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException, IOException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(cluster.getId());
            if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_9_2)) {
                apiClient = clouderaManagerApiClientProvider.getV51Client(stack.getGatewayPort(), user, password, clientConfig);
            } else if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
                apiClient = clouderaManagerApiClientProvider.getV40Client(stack.getGatewayPort(), user, password, clientConfig);
            } else {
                apiClient = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
            }
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public void waitForServer(boolean defaultClusterManagerAuth) throws CloudbreakException, ClusterClientInitException {
        ApiClient client = defaultClusterManagerAuth ? createApiClient() : apiClient;
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, client);
        if (pollingResult.isSuccess()) {
            LOGGER.debug("Cloudera Manager server has successfully started! Polling result: {}", pollingResult);
        } else if (pollingResult.isExited()) {
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
            Set<InstanceMetadataView> instances = instanceMetaDataByHostGroup.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            waitForHosts(instances);
            String sdxContextName = Optional.ofNullable(sdxContext).map(this::setupRemoteDataContext).orElse(null);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
            ApiClusterTemplate apiClusterTemplate = getCmTemplate(templatePreparationObject, sdxContextName, instanceMetaDataByHostGroup,
                    clouderaManagerRepoDetails, clusterId);

            return getExtendedBlueprintText(apiClusterTemplate);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public void validateLicence() {
        try {
            clouderaManagerLicenseService.validateClouderaManagerLicense(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public void configureManagementServices(TemplatePreparationObject templatePreparationObject,
            String sdxContext,
            String sdxStackCrn,
            Telemetry telemetry,
            ProxyConfig proxyConfig) {
        String sdxContextName = Optional.ofNullable(sdxContext).map(this::setupRemoteDataContext).orElse(null);
        try {
            configureCmMgmtServices(templatePreparationObject, sdxStackCrn, telemetry, sdxContextName, proxyConfig);
        } catch (ApiException e) {
            throw mapApiException(e);
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public void configureSupportTags(TemplatePreparationObject templatePreparationObject) {
        try {
            configureCmSupportTag(templatePreparationObject);
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public void suppressWarnings() {
        clouderaManagerYarnSetupService.suppressWarnings(stack, apiClient);
    }

    @Override
    public void startManagementServices() {
        try {
            clouderaManagerMgmtLaunchService.startManagementServices(stack, apiClient);
        } catch (ApiException e) {
            throw mapApiException(e);
        }
    }

    @Override
    public void configureKerberos(KerberosConfig kerberosConfig) {
        ClusterView cluster = stack.getCluster();
        try {
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(cluster.getId());
            if (!CMRepositoryVersionUtil.isEnableKerberosSupportedViaBlueprint(clouderaManagerRepoDetails)) {
                kerberosService.configureKerberosViaApi(apiClient, clientConfig, stack, kerberosConfig);
            }
        } catch (ApiException e) {
            throw mapApiException(e);
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    @Override
    public void installCluster(String template) {
        ClusterView cluster = stack.getCluster();
        try {
            Optional<ApiCluster> cmCluster = getCmClusterByName(cluster.getName());
            boolean prewarmed = isPrewarmed(cluster.getId());
            Optional<ClusterCommand> importCommand = clusterCommandService.findTopByClusterIdAndClusterCommandType(cluster.getId(),
                    ClusterCommandType.IMPORT_CLUSTER);
            if (cmCluster.isEmpty() || importCommand.isEmpty()) {
                importCommand = initTemplateInstallInCm(template, prewarmed, cluster);
            } else if (importCommand.isPresent()) {
                retryTemplateInstallIfNeeded(importCommand.get());
            }
            importCommand.ifPresent(cmd -> clouderaManagerPollingServiceProvider.startPollingCmTemplateInstallation(stack, apiClient, cmd.getCommandId()));
        } catch (ApiException e) {
            String msg = "Installation of CDP with Cloudera Manager has failed: " + extractMessage(e);
            throw new ClouderaManagerOperationFailedException(msg, e);
        } catch (CloudStorageConfigurationFailedException e) {
            LOGGER.info("Error while configuring cloud storage. Message: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(mapStorageError(e, stack.getResourceCrn(), stack.getCloudPlatform(), cluster), e);
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    private Optional<ClusterCommand> initTemplateInstallInCm(String template, boolean prewarmed, ClusterView cluster) throws IOException, ApiException {
        Optional<ClusterCommand> importCommand;
        ApiClusterTemplate apiClusterTemplate = JsonUtil.readValue(template, ApiClusterTemplate.class);
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        LOGGER.info("Generated Cloudera cluster template: {}", AnonymizerUtil.anonymize(template));
        // addRepositories - if true the parcels repositories in the cluster template
        // will be added.
        ApiCommand apiCommand = clouderaManagerResourceApi
                .importClusterTemplate(apiClusterTemplate, calculateAddRepositories(apiClusterTemplate, prewarmed));
        ClusterCommand clusterCommand = new ClusterCommand();
        clusterCommand.setClusterId(cluster.getId());
        clusterCommand.setCommandId(apiCommand.getId());
        clusterCommand.setClusterCommandType(ClusterCommandType.IMPORT_CLUSTER);
        importCommand = Optional.of(clusterCommandService.save(clusterCommand));
        LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");
        return importCommand;
    }

    private void retryTemplateInstallIfNeeded(ClusterCommand importCommand) throws ApiException {
        ApiCommand apiCommand = clouderaManagerCommandsService.getApiCommand(apiClient, importCommand.getCommandId());
        if (apiCommand != null && isFalse(apiCommand.isSuccess()) && isFalse(apiCommand.isActive()) && isTrue(apiCommand.isCanRetry())) {
            ApiCommand retriedApiCommand = clouderaManagerCommandsService.retryApiCommand(apiClient, importCommand.getCommandId());
            importCommand.setCommandId(retriedApiCommand.getId());
            clusterCommandService.save(importCommand);
        }
    }

    @Override
    public void publishPolicy(String template, boolean govCloud) {
        try {
            ApiClusterTemplate apiClusterTemplate = JsonUtil.readValue(template, ApiClusterTemplate.class);
            if (isVersionNewerOrEqualThanLimited(apiClusterTemplate.getCmVersion(), CLOUDERAMANAGER_VERSION_7_9_2)) {
                LOGGER.info("Configuring API policy for Cloudera Manager...");
                ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
                List<ApiConfigEnforcement> apiConfigEnforcements = new ArrayList<>(clouderaManagerCipherService.getApiConfigEnforcements());
                if (govCloud) {
                    LOGGER.info("Adding FedRAMP related enforcements to the policy...");
                    apiConfigEnforcements.addAll(clouderaManagerFedRAMPService.getApiConfigEnforcements());
                }
                ApiConfigPolicy apiConfigPolicy = new ApiConfigPolicy()
                        .version(POLICY_VERSION)
                        .name(govCloud ? POLICY_NAME_GOV : POLICY_NAME)
                        .description(govCloud ? POLICY_DESCRIPTION_GOV : POLICY_DESCRIPTION)
                        .configEnforcements(apiConfigEnforcements);
                clouderaManagerResourceApi.addConfigPolicy(apiConfigPolicy);
            }
        } catch (Exception e) {
            LOGGER.error("Policy configuration error occurred.", e);
            throw mapException(e);
        }
    }

    private String mapStorageError(CloudStorageConfigurationFailedException exception, String stackCrn, String cloudPlatform, ClusterView cluster) {
        String accountId = Crn.safeFromString(stackCrn).getAccountId();
        String result = exception.getMessage();
        if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform) && !entitlementService.awsCloudStorageValidationEnabled(accountId) ||
                CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform) && !entitlementService.azureCloudStorageValidationEnabled(accountId) ||
                CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && !entitlementService.gcpCloudStorageValidationEnabled(accountId)) {
            result = clouderaManagerStorageErrorMapper.map(exception, cloudPlatform, cluster);
        }
        return result;
    }

    @Override
    public void autoConfigureClusterManager() {
        MgmtServiceResourceApi mgmtApi = clouderaManagerApiFactory.getMgmtServiceResourceApi(apiClient);
        try {
            mgmtApi.autoConfigure();
        } catch (ApiException e) {
            String msg = "Error happened when CM autoconfigure was called: " + extractMessage(e);
            LOGGER.error(msg, e);
            throw new ClouderaManagerOperationFailedException(msg, e);
        }
    }

    @Override
    public void updateConfig() {
        com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType stackType = stack.getType();
        try {
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
            ApiConfigList apiConfigList = new ApiConfigList()
                    .addItemsItem(removeRemoteParcelRepos())
                    .addItemsItem(setHeader(stackType))
                    .addItemsItem(disableGoogleAnalytics());
            clouderaManagerResourceApi.updateConfig(apiConfigList, "Updated configurations.");
            updateClouderaManagerMonitoringConfig();
        } catch (ApiException e) {
            throw mapApiException(e);
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    private void updateClouderaManagerMonitoringConfig() throws ApiException {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if ((entitlementService.isObservabilitySaasPremiumEnabled(accountId) || entitlementService.isObservabilitySaasTrialEnabled(accountId)) &&
                entitlementService.isObservabilityRealTimeJobsEnabled(accountId)) {
            LOGGER.debug("Trying to set CM parameter 'enable_observability_real_time_jobs'.");
            updateCmConfigSilentlyIfNotSupportedByCm("enable_observability_real_time_jobs", "true");
        }
        if (entitlementService.isObservabilityDmpEnabled(accountId)) {
            LOGGER.debug("Trying to set CM parameter 'enable_observability_metrics_dmp'.");
            updateCmConfigSilentlyIfNotSupportedByCm("enable_observability_metrics_dmp", "true");
        }
    }

    private void updateCmConfigSilentlyIfNotSupportedByCm(String parameterName, String value) throws ApiException {
        try {
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
            ApiConfigList apiConfigList = new ApiConfigList().addItemsItem(new ApiConfig().name(parameterName).value(value));
            clouderaManagerResourceApi.updateConfig(apiConfigList, "Updated configurations.");
        } catch (ApiException e) {
            if (e.getCode() == BAD_REQUEST_ERROR_CODE && e.getResponseBody() != null && e.getResponseBody().contains("Unknown configuration attribute")) {
                LOGGER.debug("CM parameter '{}' is not supported by this CM version.", parameterName);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void refreshParcelRepos() {
        try {
            boolean prewarmed = isPrewarmed(stack.getCluster().getId());
            if (prewarmed) {
                ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
                ApiCommand apiCommand = clouderaManagerResourceApi.refreshParcelRepos();
                clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClient, apiCommand.getId());
            }
        } catch (Exception e) {
            LOGGER.info("Unable to refresh parcel repo", e);
            throw new CloudbreakServiceException(e);
        }
    }

    @Override
    public ExtendedPollingResult waitForHosts(Set<InstanceMetadataView> hostsInCluster) throws ClusterClientInitException {
        ApiClient client = createApiClienForWaitingForHosts();
        List<String> privateIps = hostsInCluster.stream().map(InstanceMetadataView::getPrivateIp).collect(Collectors.toList());
        return clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, client, privateIps);
    }

    private ApiClient createApiClienForWaitingForHosts() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            return clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public ExtendedPollingResult waitForHostsHealthy(Set<InstanceMetadataView> hostsInCluster) throws ClusterClientInitException {
        ApiClient client = createApiClienForWaitingForHosts();
        return clouderaManagerPollingServiceProvider.startPollingCmHostStatusHealthy(
                stack, client, hostsInCluster);
    }

    @Override
    public String getSdxContext() {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient rootClient = clouderaManagerApiClientProvider.getRootClient(stack.getGatewayPort(), user, password, clientConfig);
            CdpResourceApi cdpResourceApi = clouderaManagerApiFactory.getCdpResourceApi(rootClient);
            LOGGER.info("Get remote context from Datalake cluster: {}", stack.getName());
            ApiRemoteDataContext remoteDataContext = cdpResourceApi.getRemoteContextByCluster(stack.getName());
            return JsonUtil.writeValueAsString(remoteDataContext);
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.error("Error while getting remote context of Datalake cluster: {}", stack.getName(), e);
            throw new ClouderaManagerOperationFailedException("Error while getting remote context of Datalake cluster", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize remote context.", e);
            throw new ClouderaManagerOperationFailedException("Failed to serialize remote context.", e);
        }
    }

    @Override
    public void setupProxy(ProxyConfig proxyConfig) {
        LOGGER.info("Setup proxy for CM");
        Optional<ProxyConfig> optionalProxy = Optional.ofNullable(proxyConfig);
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
        ApiConfigList proxyConfigList = new ApiConfigList();
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_server")
                .value(optionalProxy.map(ProxyConfig::getServerHost).orElse("")));
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_port")
                .value(optionalProxy.map(pc -> String.valueOf(pc.getServerPort())).orElse("")));
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_protocol")
                .value(optionalProxy.map(pc -> pc.getProtocol().toUpperCase(Locale.ROOT)).orElse("")));

        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_user")
                .value(optionalProxy.flatMap(ProxyConfig::getProxyAuthentication).map(ProxyAuthentication::getUserName).orElse("")));
        proxyConfigList.addItemsItem(new ApiConfig().name("parcel_proxy_password")
                .value(optionalProxy.flatMap(ProxyConfig::getProxyAuthentication).map(ProxyAuthentication::getPassword).orElse("")));
        addNoProxyHosts(proxyConfig, proxyConfigList);
        try {
            LOGGER.info("Update settings with: " + proxyConfigList);
            clouderaManagerResourceApi.updateConfig(proxyConfigList, "Update proxy settings");
        } catch (ApiException e) {
            String failMessage = "Update proxy settings failed";
            LOGGER.error(failMessage, e);
            throw new ClouderaManagerOperationFailedException(failMessage, e);
        }
    }

    private void addNoProxyHosts(ProxyConfig proxyConfig, ApiConfigList proxyConfigList) {
        ClusterView cluster = stack.getCluster();
        ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(cluster.getId());
        if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_6_0)) {
            String noProxyHosts = Optional.ofNullable(proxyConfig).map(ProxyConfig::getNoProxyHosts).orElse("");
            proxyConfigList.addItemsItem(new ApiConfig().name("parcel_no_proxy_list").value(noProxyHosts));
        }
    }

    @Override
    public String setupRemoteDataContext(String sdxContext) {
        ClusterView cluster = stack.getCluster();
        String user = cluster.getCloudbreakClusterManagerUser();
        String password = cluster.getCloudbreakClusterManagerPassword();
        try {
            ApiClient rootClient = clouderaManagerApiClientProvider.getRootClient(stack.getGatewayPort(), user, password, clientConfig);
            CdpResourceApi cdpResourceApi = clouderaManagerApiFactory.getCdpResourceApi(rootClient);
            ApiRemoteDataContext apiRemoteDataContext = JsonUtil.readValue(sdxContext, ApiRemoteDataContext.class);
            LOGGER.info("Posting remote context to workload. EndpointId: {}", apiRemoteDataContext.getEndPointId());
            return cdpResourceApi.postRemoteContext(apiRemoteDataContext).getEndPointId();
        } catch (ApiException | ClouderaManagerClientInitException e) {
            LOGGER.info("Error while creating data context using: {}", sdxContext, e);
            throw new ClouderaManagerOperationFailedException(String.format("Error while creating data context: %s", e.getMessage()), e);
        } catch (IOException e) {
            LOGGER.info("Failed to parse SDX context to CM API object.", e);
            throw new ClouderaManagerOperationFailedException(String.format("Failed to parse SDX context to CM API object: %s", e.getMessage()), e);
        }
    }

    private ClouderaManagerOperationFailedException mapApiException(ApiException e) {
        LOGGER.info("Error while building the cluster. Message: {}; Response: {}", e.getMessage(), e.getResponseBody(), e);
        String msg = extractMessage(e);
        return new ClouderaManagerOperationFailedException(msg, e);
    }

    private ClouderaManagerOperationFailedException mapException(Exception e) {
        LOGGER.info("Error while building the cluster. Message: {}", e.getMessage(), e);
        return new ClouderaManagerOperationFailedException(e.getMessage(), e);
    }

    private ApiClient createApiClient() throws ClusterClientInitException {
        ApiClient client = null;
        try {
            client = clouderaManagerApiClientProvider.getDefaultClient(stack.getGatewayPort(), clientConfig, ClouderaManagerApiClientProvider.API_V_31);
            ToolsResourceApi toolsResourceApi = clouderaManagerApiFactory.getToolsResourceApi(client);
            toolsResourceApi.echo("TEST");
            LOGGER.debug("Cloudera Manager already running, old admin user's password has not been changed yet.");
            return client;
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        } catch (ApiException e) {
            return returnClientBasedOnApiError(client, e);
        }
    }

    private ApiClient returnClientBasedOnApiError(ApiClient client, ApiException e) {
        if (org.springframework.http.HttpStatus.UNAUTHORIZED.value() == e.getCode()) {
            LOGGER.debug("Cloudera Manager already running, old admin user's password has been changed.");
            return apiClient;
        }
        LOGGER.debug("Cloudera Manager is not running yet.", e);
        return client;
    }

    private ApiClusterTemplate getCmTemplate(TemplatePreparationObject templatePreparationObject, String sdxContextName,
            Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, ClouderaManagerRepo clouderaManagerRepoDetails, Long clusterId) {
        List<ClouderaManagerProduct> clouderaManagerProductDetails = clusterComponentProvider.getClouderaManagerProductDetails(clusterId);
        Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);
        return cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings, clouderaManagerRepoDetails,
                clouderaManagerProductDetails, sdxContextName);
    }

    private void configureCmMgmtServices(TemplatePreparationObject templatePreparationObject, String sdxCrn, Telemetry telemetry,
            String sdxContextName, ProxyConfig proxyConfig) throws ApiException {
        Optional<ApiHost> optionalCmHost;
        if (null != templatePreparationObject.getStackType()
                && templatePreparationObject.getStackType().equals(StackType.DATALAKE)
                && isVersionNewerOrEqualThanLimited(templatePreparationObject.getBlueprintView().getVersion(), CLOUDERA_STACK_VERSION_7_2_17)
                && blueprintUtils.isEnterpriseDatalake(templatePreparationObject)) {
            LOGGER.info("CM MGMT services are started on Auxiliary host group");
            optionalCmHost = getAuxiliaryHost(templatePreparationObject, apiClient);
        } else {
            LOGGER.info("CM MGMT services are started on Gateway host group");
            optionalCmHost = getCmHost(templatePreparationObject, apiClient);
        }
        if (optionalCmHost.isPresent()) {
            ApiHost cmHost = optionalCmHost.get();
            ApiHostRef cmHostRef = new ApiHostRef();
            cmHostRef.setHostId(cmHost.getHostId());
            cmHostRef.setHostname(cmHost.getHostname());
            mgmtSetupService.setupMgmtServices(stack, apiClient, cmHostRef,
                    telemetry, sdxContextName, sdxCrn, proxyConfig);
        } else {
            LOGGER.warn("Unable to determine Cloudera Manager host. Skipping management services installation for {}.", sdxCrn);
        }
    }

    @Override
    public void updateSmonConfigs(Telemetry telemetry) {
        try {
            mgmtSetupService.updateSmonConfigs(stack, apiClient, telemetry);
        } catch (ApiException e) {
            throw mapApiException(e);
        }
    }

    private void configureCmSupportTag(TemplatePreparationObject templatePreparationObject) throws ApiException {
        Optional<ApiHost> optionalCmHost = getCmHost(templatePreparationObject, apiClient);
        if (optionalCmHost.isPresent()) {
            clouderaManagerSupportSetupService.prepareSupportRole(apiClient, stack.getType());
        } else {
            LOGGER.warn("Unable to determine Cloudera Manager host. Skipping support tag setup.");
        }
    }

    private Optional<ApiHost> getCmHost(TemplatePreparationObject templatePreparationObject, ApiClient apiClient) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        return hostsResourceApi.readHosts(null, null, DataView.SUMMARY.name())
                .getItems()
                .stream()
                .filter(host -> host.getHostname().equals(templatePreparationObject.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN().get()))
                .findFirst();
    }

    public Optional<ApiHost> getAuxiliaryHost(TemplatePreparationObject templatePreparationObject, ApiClient apiClient) throws ApiException {
        HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(apiClient);
        Optional<ApiHost> auxiliaryHost = hostsResourceApi.readHosts(null, null, DataView.SUMMARY.name())
                .getItems()
                .stream()
                .filter(host -> (host.getHostname().toUpperCase(Locale.ROOT).contains("AUXILIARY")))
                .findFirst();
        if (auxiliaryHost.isPresent()) {
            LOGGER.info("Get Auxiliary host for CM MGMT services. Host: {}", auxiliaryHost.get());
        }
        return auxiliaryHost;
    }

    private boolean calculateAddRepositories(ApiClusterTemplate apiClusterTemplate, boolean prewarmed) {
        boolean addRepositories = !prewarmed;
        if (apiClusterTemplate.getCmVersion() != null
                && isVersionNewerOrEqualThanLimited(apiClusterTemplate.getCmVersion(), CLOUDERAMANAGER_VERSION_7_2_0)) {
            addRepositories = true;
        }
        return addRepositories;
    }

    private Optional<ApiCluster> getCmClusterByName(String name) throws ApiException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        try {
            return Optional.of(clustersResourceApi.readCluster(name));
        } catch (ApiException apiException) {
            if (apiException.getCode() != HttpStatus.SC_NOT_FOUND) {
                throw apiException;
            }
            return Optional.empty();
        }
    }

    private ApiConfig removeRemoteParcelRepos() {
        return new ApiConfig().name("remote_parcel_repo_urls").value("");
    }

    private ApiConfig disableGoogleAnalytics() {
        return new ApiConfig().name("allow_usage_data").value("false");
    }

    private ApiConfig setHeader(com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType stackType) {
        return new ApiConfig().name("custom_header_color").value(StackType.DATALAKE.equals(stackType) ? "RED" : "BLUE");
    }

    private String getExtendedBlueprintText(ApiClusterTemplate apiClusterTemplate) {
        return JsonUtil.writeValueAsStringSilent(apiClusterTemplate);
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
