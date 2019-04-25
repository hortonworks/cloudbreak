package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_HOST_JOIN_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_INSTALL_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTED;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.INSTALL_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.START_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.START_OPERATION_STATE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintUpdater;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
@Scope("prototype")
public class AmbariClusterSetupService implements ClusterSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterSetupService.class);

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Inject
    private AmbariClusterTemplateSubmitter ambariClusterTemplateSubmitter;

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
    private AmbariAdapter ambariAdapter;

    private AmbariClient ambariClient;

    public AmbariClusterSetupService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void waitForServer() throws CloudbreakException {
        AmbariClient defaultClient = ambariClientFactory.getDefaultAmbariClient(stack, clientConfig);
        PollingResult pollingResult = ambariPollingServiceProvider.ambariStartupPollerObjectPollingService(stack, defaultClient, ambariClient);
        if (isSuccess(pollingResult)) {
            LOGGER.debug("Ambari has successfully started! Polling result: {}", pollingResult);
        } else if (isExited(pollingResult)) {
            throw new CancellationException("Polling of Ambari server start has been cancelled.");
        } else {
            LOGGER.debug("Could not start Ambari. polling result: {}", pollingResult);
            throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s'", pollingResult));
        }
    }

    @Override
    public Cluster buildCluster(Map<HostGroup, List<InstanceMetaData>> instanceMetaDataByHostGroup, TemplatePreparationObject templatePreparationObject,
            Set<HostMetadata> hostsInCluster) {
        Cluster cluster = stack.getCluster();
        try {
            ambariRepositoryVersionService.setBaseRepoURL(stack.getName(), cluster.getId(), ambariClient);
            String blueprintText = centralBlueprintUpdater.getBlueprintText(templatePreparationObject);
            addBlueprint(stack.getId(), ambariClient, blueprintText, cluster.getTopologyValidation());
            PollingResult waitForHostsResult = ambariPollingServiceProvider.hostsPollingService(stack, ambariClient, hostsInCluster);
            clusterConnectorPollingResultChecker
                    .checkPollingResult(waitForHostsResult, cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_HOST_JOIN_FAILED.code()));
            Map<String, List<Map<String, String>>> hostGroupMappings = hostGroupAssociationBuilder.buildHostGroupAssociations(instanceMetaDataByHostGroup);
            ambariClusterTemplateSubmitter.addClusterTemplate(cluster, hostGroupMappings, ambariClient);
            Pair<PollingResult, Exception> pollingResult =
                    ambariOperationService.waitForOperationsToStart(stack, ambariClient, singletonMap("INSTALL_START", 1), START_OPERATION_STATE);

            String message =
                    pollingResult.getRight() == null ? constructClusterFailedMessage(cluster.getId(), ambariClient) : pollingResult.getRight().getMessage();
            clusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
            Pair<PollingResult, Exception> pollingResultExceptionPair = ambariOperationService
                    .waitForOperations(stack, ambariClient, Map.of("CLUSTER_INSTALL", 1), INSTALL_AMBARI_PROGRESS_STATE);

            clusterConnectorPollingResultChecker
                    .checkPollingResult(pollingResultExceptionPair.getLeft(), constructClusterFailedMessage(cluster.getId(), ambariClient));
            return ambariViewProvider.provideViewInformation(ambariClient, cluster);
        } catch (CancellationException cancellationException) {
            throw cancellationException;
        } catch (Exception e) {
            LOGGER.warn("Error while building the Ambari cluster. Message {}, throwable: {}", e.getMessage(), e);
            throw new AmbariOperationFailedException(e.getMessage(), e);
        }
    }

    private String constructClusterFailedMessage(Long clusterId, AmbariClient ambariClient) {
        String ambariClusterInstallFailedMsg = cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_INSTALL_FAILED.code());
        ClusterStatusResult clusterStatusResult = ambariAdapter.getClusterStatusHostComponentMap(ambariClient);
        String statusReason = clusterStatusResult.getStatusReason();
        LOGGER.debug("There are not started services. Cluster: [{}], services: [{}]", clusterId, statusReason);
        return String.format("%s Not started services: [%s]", ambariClusterInstallFailedMsg, statusReason);
    }

    @Override
    public void waitForHosts(Set<HostMetadata> hostsInCluster) {
        ambariPollingServiceProvider
                .hostsPollingService(
                        stack,
                        ambariClient,
                        hostsInCluster);
    }

    @Override
    public void waitForServices(int requestId) throws CloudbreakException {
        LOGGER.debug("Waiting for Hadoop services to start on stack");
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

    private void addBlueprint(Long stackId, AmbariClient ambariClient, String blueprintText, Boolean topologyValidation) {
        try {
            LOGGER.debug("Adding generated blueprint to Ambari: {}", JsonUtil.minify(blueprintText));
            ambariClient.addBlueprint(blueprintText, topologyValidation);
        } catch (HttpResponseException hre) {
            if (hre.getStatusCode() == HttpStatus.SC_CONFLICT) {
                LOGGER.debug("Ambari blueprint already exists for stack: {}", stackId);
            } else {
                throw new CloudbreakServiceException("Ambari blueprint could not be added: " + AmbariClientExceptionUtil.getErrorMessage(hre), hre);
            }
        } catch (URISyntaxException | IOException e) {
            throw new CloudbreakServiceException("Ambari blueprint could not be added: " + e.getMessage(), e);
        }
    }

}
