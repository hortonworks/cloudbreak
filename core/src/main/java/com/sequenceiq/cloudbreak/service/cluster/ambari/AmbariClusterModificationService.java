package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTING_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPING;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPING_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_UPSCALE_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.HostGroupAssociationBuilder.FQDN;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientRetryer;
import com.sequenceiq.cloudbreak.service.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariClusterModificationService implements ClusterModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterModificationService.class);

    private static final String KEY_OF_RACK_INFO = "rack";

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private AmbariClusterConnectorPollingResultChecker ambariClusterConnectorPollingResultChecker;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Inject
    private AmbariClientRetryer ambariClientRetryer;

    @Override
    public void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        List<String> clusterHosts = ambariClientRetryer.getClusterHosts(ambariClient);

        List<String> upscaleHostNames = hostMetadata
                .stream()
                .map(HostMetadata::getHostName)
                .collect(Collectors.toList())
                .stream()
                .filter(hostName -> !clusterHosts.contains(hostName))
                .collect(Collectors.toList());

        if (!upscaleHostNames.isEmpty()) {
            try {
                recipeEngine.executePostAmbariStartRecipes(stack, Sets.newHashSet(hostGroup));
                Pair<PollingResult, Exception> pollingResult = ambariOperationService.waitForOperations(
                        stack,
                        ambariClient,
                        installServices(upscaleHostNames, stack, ambariClient, hostGroup),
                        UPSCALE_AMBARI_PROGRESS_STATE);
                String message = pollingResult.getRight() == null
                        ? cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_UPSCALE_FAILED.code())
                        : pollingResult.getRight().getMessage();
                ambariClusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
            } catch (IOException | URISyntaxException e) {
                throw new CloudbreakException("Failed to upscale cluster.", e);
            }
        }
    }

    @Override
    public void stopCluster(Stack stack) {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        try {
            if (!isClusterStopped(ambariClient)) {
                stopHadoopServices(stack, ambariClient);
            }
        } catch (AmbariConnectionException ignored) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    private boolean isClusterStopped(AmbariClient ambariClient) {
        Collection<Map<String, String>> values = ambariClientRetryer.getHostComponentsStates(ambariClient).values();
        for (Map<String, String> value : values) {
            for (String state : value.values()) {
                if (!"INSTALLED".equals(state)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void stopHadoopServices(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Stop all Hadoop services");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                        cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPING.code()));
        try {
            int requestId = ambariClientRetryer.stopAllServices(ambariClient);
            if (requestId != -1) {
                waitForServicesToStop(stack, ambariClient, requestId);
            } else {
                String errorMessage = "Ambari returned '-1' for stop all services call";
                LOGGER.error(errorMessage);
                sendNotification(stack, AMBARI_CLUSTER_SERVICES_STOPPING_FAILED, errorMessage);
            }
            eventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                            cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPED.code()));
        } catch (RuntimeException | IOException | URISyntaxException | CloudbreakException e) {
            LOGGER.error("Cloudbreak can not stop services on Ambari side", e);
            sendNotification(stack, AMBARI_CLUSTER_SERVICES_STOPPING_FAILED, ExceptionUtils.getStackTrace(e));
        }
    }

    private void waitForServicesToStop(Stack stack, AmbariClient ambariClient, int requestId) throws CloudbreakException {
        LOGGER.info("Waiting for Hadoop services to stop on stack");
        PollingResult servicesStopResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("stop services", requestId),
                STOP_AMBARI_PROGRESS_STATE).getLeft();
        if (isExited(servicesStopResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(servicesStopResult)) {
            throw new CloudbreakException("Timeout while stopping Ambari services.");
        }
    }

    @Override
    public int startCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        PollingResult ambariHealthCheckResult = ambariPollingServiceProvider.ambariHealthChecker(stack, ambariClient);
        if (isExited(ambariHealthCheckResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Ambari to start.");
        } else if (isTimeout(ambariHealthCheckResult)) {
            throw new CloudbreakException("Ambari server was not restarted properly.");
        }
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
        waitForAmbariHosts(stack, ambariClient, hostsInCluster);
        waitForComponents(stack, ambariClient, hostsInCluster);
        return startHadoopServices(stack, ambariClient);
    }

    private void waitForAmbariHosts(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster) {
        LOGGER.info("Starting Ambari agents on the hosts.");
        PollingResult hostsJoinedResult = ambariPollingServiceProvider.ambariHostJoin(stack, ambariClient, hostsInCluster);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Ambari agents.");
        }
    }

    private void waitForComponents(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster) {
        PollingResult componentsJoinedResult = ambariPollingServiceProvider.ambariComponentJoin(stack, ambariClient, hostsInCluster);
        if (isExited(componentsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop components to join.");
        }
    }

    private int startHadoopServices(Stack stack, AmbariClient ambariClient) {
        LOGGER.info("Starting all Hadoop services");
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STARTING.code()));
        int requestId = 0;
        try {
            requestId = ambariClientRetryer.startAllServices(ambariClient);
            if (requestId == -1) {
                String errorMessage = "Ambari returned '-1' for start all services call";
                sendNotification(stack, AMBARI_CLUSTER_SERVICES_STARTING_FAILED, errorMessage);
                LOGGER.error(errorMessage);
            }
        } catch (RuntimeException | IOException | URISyntaxException e) {
            LOGGER.error("Start all services failed", e);
            sendNotification(stack, AMBARI_CLUSTER_SERVICES_STARTING_FAILED, ExceptionUtils.getStackTrace(e));
        }
        return requestId;
    }

    private void sendNotification(Stack stack, AmbariMessages ambariClusterServicesStartingFailed, String errorMessage) {
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(ambariClusterServicesStartingFailed.code(), Collections.singleton(errorMessage)));
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroup hostGroup)
            throws IOException, URISyntaxException {
        try {
            String blueprintName = stack.getCluster().getBlueprint().getAmbariName();
            // In case If we changed the blueprintName field we need to query the validation name information from ambari
            Map<String, String> blueprintsMap = ambariClientRetryer.getBlueprintsMap(ambariClient);
            if (!blueprintsMap.entrySet().isEmpty()) {
                blueprintName = blueprintsMap.keySet().iterator().next();
            }
            List<Map<String, String>> hostGroupAssociation = hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup);
            int upscaleRequestCode = addHosts(hosts, ambariClient, hostGroup, blueprintName, hostGroupAssociation);
            return singletonMap("UPSCALE_REQUEST", upscaleRequestCode);
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new BadRequestException("Host already exists.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Ambari could not install services. " + errorMessage, e);
            }
        }
    }

    private int addHosts(List<String> hosts, AmbariClient client, HostGroup hostGroup, String blueprintName, List<Map<String, String>> hostInfos)
            throws URISyntaxException, IOException {
        List<Map<String, String>> hostInfoOfCandidates = hostInfos.stream()
                .filter(associationMap -> hosts.stream().anyMatch(host -> host.equals(associationMap.get(FQDN))))
                .collect(Collectors.toList());
        if (allCandidatesDoesNotHaveRackInfo(hostInfoOfCandidates)) {
            LOGGER.info("Adding hosts with blueprint to Ambari for hostgroup: '{}'", hostGroup.getName());
            List<String> hostNamesToAdd = hostInfoOfCandidates
                    .stream()
                    .map(associationMap -> associationMap.get(FQDN))
                    .collect(Collectors.toList());
            return ambariClientRetryer.addHostsWithBlueprint(client, blueprintName, hostGroup.getName(), hostNamesToAdd);
        } else {
            LOGGER.info("Adding hosts with blueprint and rack info to Ambari for hostgroup: '{}'", hostGroup.getName());
            Map<String, String> hostsWithRackInfo = hostInfoOfCandidates
                    .stream()
                    .collect(Collectors.toMap(association -> association.get(FQDN), association ->
                            association.get(KEY_OF_RACK_INFO) != null ? association.get(KEY_OF_RACK_INFO) : "/default-rack"));
            return ambariClientRetryer.addHostsAndRackInfoWithBlueprint(client, blueprintName, hostGroup.getName(), hostsWithRackInfo);
        }
    }

    private boolean allCandidatesDoesNotHaveRackInfo(List<Map<String, String>> hostInfoForCandidates) {
        return hostInfoForCandidates
                .stream()
                .allMatch(associationMap -> StringUtils.isEmpty(associationMap.get(KEY_OF_RACK_INFO)));
    }

}
