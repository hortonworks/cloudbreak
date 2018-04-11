package com.sequenceiq.cloudbreak.service.cluster.ambari;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import groovyx.net.http.HttpResponseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.*;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.*;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.*;
import static java.util.Collections.singletonMap;

@Service
public class AmbariClusterSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterSetupService.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private AmbariUserHandler ambariUserHandler;

    @Inject
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Inject
    private AmbariClusterTemplateService ambariClusterTemplateService;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Inject
    private CentralBlueprintUpdater centralBlueprintUpdater;

    @Inject
    private AmbariClusterCreationSuccessHandler ambariClusterCreationSuccessHandler;

    @Inject
    private AmbariSmartSenseCapturer ambariSmartSenseCapturer;

    @Override
    public void waitForServer(Stack stack) throws CloudbreakException {
        AmbariClient defaultClient = clientFactory.getDefaultAmbariClient(stack);
        AmbariClient client = clientFactory.getAmbariClient(stack, stack.getCluster());
        PollingResult pollingResult = ambariPollingServiceProvider.ambariStartupPollerObjectPollingService(stack, defaultClient, client);
        if (isSuccess(pollingResult)) {
            LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
        } else if (isExited(pollingResult)) {
            throw new CancellationException("Polling of Ambari server start has been cancelled.");
        } else {
            LOGGER.info("Could not start Ambari. polling result: {}", pollingResult);
            throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s'", pollingResult));
        }
    }

    @Override
    public void buildCluster(Stack stack) {
        Cluster cluster = stack.getCluster();
        try {
            clusterService.updateCreationDateOnCluster(cluster);
            AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
            PreparationObject blueprintPreparationObject = conversionService.convert(stack, PreparationObject.class);
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(hostGroups);
            Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(cluster.getId());

            recipeEngine.executePostAmbariStartRecipes(stack, hostGroups);
            ambariRepositoryVersionService.setBaseRepoURL(stack.getName(), cluster.getId(), stack.getOrchestrator(), ambariClient);
            String blueprintText = centralBlueprintUpdater.getBlueprintText(blueprintPreparationObject);
            addBlueprint(stack.getId(), ambariClient, blueprintText, cluster.getTopologyValidation());
            cluster.setExtendedBlueprintText(blueprintText);
            clusterService.updateCluster(cluster);
            PollingResult waitForHostsResult = ambariPollingServiceProvider.hostsPollingService(stack, ambariClient, hostsInCluster);
            ambariClusterConnectorPollingResultChecker
                    .checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));
            ambariClusterTemplateService.addClusterTemplate(cluster, hostGroupMappings, ambariClient);
            Pair<PollingResult, Exception> pollingResult =
                    ambariOperationService.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1), START_OPERATION_STATE);
            String message = pollingResult.getRight() == null
                    ? cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_INSTALL_FAILED.code()) : pollingResult.getRight().getMessage();
            ambariClusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
            Pair<PollingResult, Exception> pollingResultExceptionPair = ambariOperationService
                    .waitForOperations(stack, ambariClient, new HashMap<String, Integer>() { {
                        put("CLUSTER_INSTALL", 1);
                    } }, INSTALL_AMBARI_PROGRESS_STATE);
            ambariClusterConnectorPollingResultChecker
                    .checkPollingResult(pollingResultExceptionPair.getLeft(), cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_INSTALL_FAILED.code()));
            recipeEngine.executePostInstall(stack);
            ambariSmartSenseCapturer.capture(0, ambariClient);
            cluster = ambariViewProvider.provideViewInformation(ambariClient, cluster);
            ambariClusterCreationSuccessHandler.handleClusterCreationSuccess(stack, cluster);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (HttpResponseException hre) {
            throw new AmbariOperationFailedException("Ambari could not create the cluster: " + AmbariClientExceptionUtil.getErrorMessage(hre), hre);
        } catch (Exception e) {
            LOGGER.error("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void waitForHosts(Stack stack) throws CloudbreakSecuritySetupException {
        ambariPollingServiceProvider
                .hostsPollingService(
                        stack,
                        clientFactory.getAmbariClient(stack, stack.getCluster()),
                        hostMetadataRepository.findHostsInCluster(stack.getCluster().getId()));
    }

    @Override
    public void waitForServices(Stack stack, int requestId) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        LOGGER.info("Waiting for Hadoop services to start on stack");
        PollingResult servicesStartResult = ambariOperationService
                .waitForOperations(stack, ambariClient, singletonMap("start services", requestId), START_AMBARI_PROGRESS_STATE).getLeft();
        if (isExited(servicesStartResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(servicesStartResult)) {
            throw new CloudbreakException("Timeout while starting Ambari services.");
        }
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STARTED.code()));
    }

    @Override
    public boolean available(Stack stack) throws CloudbreakSecuritySetupException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        return ambariPollingServiceProvider.isAmbariAvailable(stack, ambariClient);
    }

    private void addBlueprint(Long stackId, AmbariClient ambariClient, String blueprintText, Boolean topologyValidation) {
        try {
            LOGGER.info("Adding generated blueprint to Ambari: {}", JsonUtil.minify(blueprintText));
            ambariClient.addBlueprint(blueprintText, topologyValidation);
        } catch (HttpResponseException hre) {
            if (hre.getStatusCode() == HttpStatus.SC_CONFLICT) {
                LOGGER.info("Ambari blueprint already exists for stack: {}", stackId);
            } else {
                throw new CloudbreakServiceException("Ambari blueprint could not be added: " + AmbariClientExceptionUtil.getErrorMessage(hre), hre);
            }
        }
    }

}
