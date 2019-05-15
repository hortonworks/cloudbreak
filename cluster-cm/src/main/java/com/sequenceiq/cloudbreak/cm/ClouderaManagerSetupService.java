package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.client.DataView;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Service
@Scope("prototype")
public class ClouderaManagerSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSetupService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ClouderaHostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private CentralCmTemplateUpdater cmTemplateUpdater;

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Inject
    private ClouderaManagerMgmtSetupService mgmtSetupService;

    @Inject
    private ClouderaManagerKerberosService kerberosService;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerSetupService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void waitForServer() throws CloudbreakException {
        ApiClient client = clouderaManagerClientFactory.getDefaultClient(stack, clientConfig);
        PollingResult pollingResult = clouderaManagerPollingServiceProvider.clouderaManagerStartupPollerObjectPollingService(stack, client);
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
    public Cluster buildCluster(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, TemplatePreparationObject templatePreparationObject,
            Set<HostMetadata> hostsInCluster, String sdxContext) {
        Cluster cluster = stack.getCluster();
        Long clusterId = cluster.getId();
        try {
            HostsResourceApi hostsResourceApi = new HostsResourceApi(client);
            Optional<ApiHost> optionalCmHost = hostsResourceApi.readHosts(DataView.SUMMARY.name()).getItems().stream().filter(
                    host -> host.getHostname().equals(templatePreparationObject.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN().get()))
                    .findFirst();

            if (optionalCmHost.isPresent()) {
                ApiHost cmHost = optionalCmHost.get();
                ApiHostRef cmHostRef = new ApiHostRef();
                cmHostRef.setHostId(cmHost.getHostId());
                cmHostRef.setHostname(cmHost.getHostname());
                mgmtSetupService.setupMgmtServices(stack, client, cmHostRef, templatePreparationObject.getRdsConfigs());
            } else {
                LOGGER.warn("Unable to determine Cloudera Manager host. Skipping management services installation.");
            }

            String sdxContextName = Optional.ofNullable(sdxContext).map(this::createDataContext).orElse(null);
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
            List<ClouderaManagerProduct> clouderaManagerProductDetails = clusterComponentProvider.getClouderaManagerProductDetails(clusterId);
            ApiClusterTemplate apiClusterTemplate = cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings, clouderaManagerRepoDetails,
                    clouderaManagerProductDetails, sdxContextName);

            cluster.setExtendedBlueprintText(getExtendedBlueprintText(apiClusterTemplate));
            LOGGER.info("Generated Cloudera cluster template: {}", cluster.getExtendedBlueprintText());
            ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);

            boolean prewarmed = isPrewarmed(clusterId);
            if (prewarmed) {
                removeRemoteParcelRepos(clouderaManagerResourceApi);
                refreshParcelRepos(clouderaManagerResourceApi);
            }
            kerberosService.setupKerberos(client, stack);
            installCluster(cluster, apiClusterTemplate, clouderaManagerResourceApi, prewarmed);
            if (!CMRepositoryVersionUtil.isEnableKerberosSupportedViaBlueprint(clouderaManagerRepoDetails)) {
                kerberosService.configureKerberosViaApi(client, clientConfig, stack, clouderaManagerRepoDetails);
            }
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.info("Error while building the cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
        return cluster;
    }

    private String createDataContext(String sdxContext) {
        ApiClient rootClient = clouderaManagerClientFactory.getRootClient(stack, stack.getCluster(), clientConfig);
        CdpResourceApi cdpResourceApi = new CdpResourceApi(rootClient);
        try {
            ApiRemoteDataContext apiRemoteDataContext = JsonUtil.readValue(sdxContext, ApiRemoteDataContext.class);
            if (remoteContextExists(cdpResourceApi, apiRemoteDataContext.getEndPointId())) {
                return apiRemoteDataContext.getEndPointId();
            }
            LOGGER.debug("Posting remote context to workload. EndpointId: {}", apiRemoteDataContext.getEndPointId());
            return cdpResourceApi.postRemoteContext(apiRemoteDataContext).getEndPointId();
        } catch (ApiException e) {
            LOGGER.info("Error while creating data context using: {}", sdxContext, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.info("Failed to parse SDX context to CM API object.", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Boolean remoteContextExists(CdpResourceApi cdpResourceApi, String dataContextName) {
        try {
            cdpResourceApi.getRemoteContext(dataContextName);
            LOGGER.debug("Remote context [{}] already exists.", dataContextName);
            return Boolean.TRUE;
        } catch (ApiException e) {
            if (org.springframework.http.HttpStatus.NOT_FOUND.value() == e.getCode()) {
                LOGGER.debug("Remote context [{}] does not exist.", dataContextName);
                return Boolean.FALSE;
            }
            LOGGER.info("Failed to check if remote context [{}] already exists.", dataContextName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void installCluster(Cluster cluster, ApiClusterTemplate apiClusterTemplate, ClouderaManagerResourceApi clouderaManagerResourceApi,
            boolean prewarmed) throws ApiException {
        ClustersResourceApi clustersResourceApi = new ClustersResourceApi(client);
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
            // addRepositories - if true the parcels repositories in the cluster template will be added.
            clusterInstallCommand = Optional.of(clouderaManagerResourceApi.importClusterTemplate(!prewarmed, apiClusterTemplate));
            LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");
        }
        clusterInstallCommand.ifPresent(cmd -> clouderaManagerPollingServiceProvider.templateInstallCheckerService(stack, client, cmd.getId()));
    }

    private void removeRemoteParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi) {
        try {
            ApiConfigList apiConfigList = new ApiConfigList()
                .addItemsItem(new ApiConfig().name("remote_parcel_repo_urls").value(""));
            clouderaManagerResourceApi.updateConfig("Updated configurations.", apiConfigList);
        } catch (ApiException e) {
            LOGGER.info("Error while updating remote parcel repos. Message {}, throwable: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void refreshParcelRepos(ClouderaManagerResourceApi clouderaManagerResourceApi) {
        try {
            ApiCommand apiCommand = clouderaManagerResourceApi.refreshParcelRepos();
            clouderaManagerPollingServiceProvider.parcelRepoRefreshCheckerService(stack, client, apiCommand.getId());
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
    public void waitForHosts(Set<HostMetadata> hostsInCluster) {
        clouderaManagerPollingServiceProvider.hostsPollingService(stack, clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig));
    }

    @Override
    public void waitForServices(int requestId) {

    }

    @Override
    public String getSdxContext() {
        ApiClient rootClient = clouderaManagerClientFactory.getRootClient(stack, stack.getCluster(), clientConfig);
        CdpResourceApi cdpResourceApi = new CdpResourceApi(rootClient);
        try {
            LOGGER.debug("Get remote context from SDX cluster: {}", stack.getName());
            ApiRemoteDataContext remoteDataContext = cdpResourceApi.getRemoteContextByCluster(stack.getName());
            return JsonUtil.writeValueAsString(remoteDataContext);
        } catch (ApiException e) {
            LOGGER.info("Error while getting remote context of SDX cluster: {}", stack.getName(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        } catch (JsonProcessingException e) {
            LOGGER.info("Failed to serialize remote context.", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Boolean isPrewarmed(Long clusterId) {
        return clusterComponentProvider.getClouderaManagerRepoDetails(clusterId).getPredefined();
    }
}
