package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.JsonUtil;

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
            Set<HostMetadata> hostsInCluster) {
        Cluster cluster = stack.getCluster();
        Long clusterId = cluster.getId();
        try {
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);
            ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
            List<ClouderaManagerProduct> clouderaManagerProductDetails = clusterComponentProvider.getClouderaManagerProductDetails(clusterId);
            ApiClusterTemplate apiClusterTemplate = cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings, clouderaManagerRepoDetails,
                    clouderaManagerProductDetails);

            cluster.setExtendedBlueprintText(getExtendedBlueprintText(apiClusterTemplate));
            LOGGER.info("Generated Cloudera cluster template: {}", cluster.getExtendedBlueprintText());
            ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);

            boolean prewarmed = isPrewarmed(clusterId);
            if (prewarmed) {
                removeRemoteParcelRepos(clouderaManagerResourceApi);
                refreshParcelRepos(clouderaManagerResourceApi);
            }
            kerberosService.setupKerberos(client, stack);
            // addRepositories - if true the parcels repositories in the cluster template will be added.
            ApiCommand apiCommand = clouderaManagerResourceApi.importClusterTemplate(!prewarmed, apiClusterTemplate);
            LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");

            clouderaManagerPollingServiceProvider.templateInstallCheckerService(stack, client, apiCommand.getId());
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

    private Boolean isPrewarmed(Long clusterId) {
        return clusterComponentProvider.getClouderaManagerRepoDetails(clusterId).getPredefined();
    }
}
