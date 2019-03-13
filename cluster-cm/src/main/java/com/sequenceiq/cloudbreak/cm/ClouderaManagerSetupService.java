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
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
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
        try {
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);

            ApiClusterTemplate apiClusterTemplate = injectCustomClusterTemplateFragments(templatePreparationObject, cluster.getId(), hostGroupMappings);
            cluster.setExtendedClusterDefinitionText(getExtendedClusterDefinitionText(apiClusterTemplate));
            LOGGER.info("Generated Cloudera cluster template: {}", cluster.getExtendedClusterDefinitionText());

            ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);
            ApiCommand apiCommand = clouderaManagerResourceApi.importClusterTemplate(true, apiClusterTemplate);
            LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");

            clouderaManagerPollingServiceProvider.templateInstallCheckerService(stack, client, apiCommand.getId());

            kerberosService.setupKerberos(client, clientConfig, stack);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.info("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
        return cluster;
    }

    private ApiClusterTemplate injectCustomClusterTemplateFragments(TemplatePreparationObject templatePreparationObject,
            Long clusterId, Map<String, List<Map<String, String>>> hostGroupMappings) {
        ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentProvider.getClouderaManagerRepoDetails(clusterId);
        List<ClouderaManagerProduct> clouderaManagerProductDetails = clusterComponentProvider.getClouderaManagerProductDetails(clusterId);
        return cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings, clouderaManagerRepoDetails, clouderaManagerProductDetails);
    }

    private ApiClusterTemplate convertStringJsonToTemplate(String json) {
        try {
            return JsonUtil.readValue(json, ApiClusterTemplate.class);
        } catch (IOException e) {
            LOGGER.info("Invalid Cloudera template json", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private String getExtendedClusterDefinitionText(ApiClusterTemplate apiClusterTemplate) {
        return JsonUtil.writeValueAsStringSilent(apiClusterTemplate);
    }

    @Override
    public void waitForHosts(Set<HostMetadata> hostsInCluster) {
        clouderaManagerPollingServiceProvider.hostsPollingService(stack, clouderaManagerClientFactory.getDefaultClient(stack, clientConfig));
    }

    @Override
    public void waitForServices(int requestId) {

    }
}
