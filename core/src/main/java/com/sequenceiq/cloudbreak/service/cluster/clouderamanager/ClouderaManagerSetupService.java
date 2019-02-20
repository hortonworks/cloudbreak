package com.sequenceiq.cloudbreak.service.cluster.clouderamanager;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationFailedException;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Service
public class ClouderaManagerSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSetupService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClouderaHostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private AmbariClusterCreationSuccessHandler ambariClusterCreationSuccessHandler;

    @Inject
    private ConversionService conversionService;

    @Inject
    private CentralCmTemplateUpdater cmTemplateUpdater;

    @Override
    public void waitForServer(Stack stack) throws CloudbreakException {
        ApiClient client = clouderaManagerClientFactory.getDefaultClient(stack);
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
    public void buildCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            ApiClient client = clouderaManagerClientFactory.getClient(stack, cluster);
            clusterService.updateCreationDateOnCluster(cluster);
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(hostGroups);

            String hostTemplate = cluster.getBlueprint().getBlueprintText();
            cluster.setExtendedBlueprintText(hostTemplate);
            clusterService.updateCluster(cluster);

            TemplatePreparationObject templatePreparationObject = conversionService.convert(stack, TemplatePreparationObject.class);
            ApiClusterTemplate apiClusterTemplate = cmTemplateUpdater.getCmTemplate(templatePreparationObject, hostGroupMappings);
            LOGGER.debug("Generated Cloudera cluster template: {}", apiClusterTemplate);

            ClouderaManagerResourceApi clouderaManagerResourceApi = new ClouderaManagerResourceApi(client);
            ApiCommand apiCommand = clouderaManagerResourceApi.importClusterTemplate(true, apiClusterTemplate);
            LOGGER.debug("Cloudera cluster template has been submitted, cluster install is in progress");

            clouderaManagerPollingServiceProvider.templateInstallCheckerService(stack, client, apiCommand.getId());

            ambariClusterCreationSuccessHandler.handleClusterCreationSuccess(stack, cluster);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.info("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void waitForHosts(Stack stack) throws CloudbreakSecuritySetupException {
        clouderaManagerPollingServiceProvider.hostsPollingService(stack, clouderaManagerClientFactory.getDefaultClient(stack));
    }

    @Override
    public void waitForServices(Stack stack, int requestId) throws CloudbreakException {

    }
}
