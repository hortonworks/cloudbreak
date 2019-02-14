package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isTimeout;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPING;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariMessages.AMBARI_CLUSTER_UPSCALE_FAILED;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.HostGroupAssociationBuilder.FQDN;
import static java.util.Collections.singletonMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.service.cluster.ClusterConnectorPollingResultChecker;
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

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

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

    @Override
    public void upscaleCluster(Stack stack, HostGroup hostGroup, Collection<HostMetadata> hostMetadata) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        List<String> upscaleHostNames = hostMetadata
                .stream()
                .map(HostMetadata::getHostName)
                .collect(Collectors.toList())
                .stream()
                .filter(hostName -> !ambariClient.getClusterHosts().contains(hostName))
                .collect(Collectors.toList());
        if (!upscaleHostNames.isEmpty()) {
            recipeEngine.executePostAmbariStartRecipes(stack, Sets.newHashSet(hostGroup));
            Pair<PollingResult, Exception> pollingResult = ambariOperationService.waitForOperations(
                    stack,
                    ambariClient,
                    installServices(upscaleHostNames, stack, ambariClient, hostGroup),
                    UPSCALE_AMBARI_PROGRESS_STATE);
            String message = pollingResult.getRight() == null
                    ? cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_UPSCALE_FAILED.code())
                    : pollingResult.getRight().getMessage();
            clusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
        }
    }

    @Override
    public void stopCluster(Stack stack) throws CloudbreakException {
        AmbariClient ambariClient = clientFactory.getAmbariClient(stack, stack.getCluster());
        try {
            boolean stopped = true;
            Collection<Map<String, String>> values = ambariClient.getHostComponentsStates().values();
            for (Map<String, String> value : values) {
                for (String state : value.values()) {
                    if (!"INSTALLED".equals(state)) {
                        stopped = false;
                    }
                }
            }
            if (!stopped) {
                LOGGER.debug("Stop all Hadoop services");
                eventService
                        .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPING.code()));
                int requestId = ambariClient.stopAllServices();
                if (requestId != -1) {
                    LOGGER.debug("Waiting for Hadoop services to stop on stack");
                    PollingResult servicesStopResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("stop services", requestId),
                            STOP_AMBARI_PROGRESS_STATE).getLeft();
                    if (isExited(servicesStopResult)) {
                        throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
                    } else if (isTimeout(servicesStopResult)) {
                        throw new CloudbreakException("Timeout while stopping Ambari services.");
                    }
                } else {
                    LOGGER.debug("Failed to stop Hadoop services.");
                    throw new CloudbreakException("Failed to stop Hadoop services.");
                }
                eventService
                        .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPED.code()));
            }
        } catch (AmbariConnectionException ignored) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
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
        LOGGER.debug("Starting Ambari agents on the hosts.");
        Set<HostMetadata> hostsInCluster = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
        PollingResult hostsJoinedResult = ambariPollingServiceProvider.ambariHostJoin(stack, ambariClient, hostsInCluster);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Ambari agents.");
        }

        LOGGER.debug("Start all Hadoop services");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STARTING.code()));
        int requestId = ambariClient.startAllServices();
        if (requestId == -1) {
            LOGGER.info("Failed to start Hadoop services.");
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
        return requestId;
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroup hostGroup) {
        try {
            String blueprintName = stack.getCluster().getClusterDefinition().getStackName();
            // In case If we changed the blueprintName field we need to query the validation name information from ambari
            Map<String, String> blueprintsMap = ambariClient.getBlueprintsMap();
            if (!blueprintsMap.entrySet().isEmpty()) {
                blueprintName = blueprintsMap.keySet().iterator().next();
            }
            List<Map<String, String>> hostGroupAssociation = hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup);
            Map<String, String> hostsWithRackInfo = hostGroupAssociation.stream()
                    .filter(associationMap -> hosts.stream().anyMatch(host -> host.equals(associationMap.get(FQDN))))
                    .collect(Collectors.toMap(association -> association.get(FQDN), association ->
                            association.get("rack") != null ? association.get("rack") : "/default-rack"));
            int upscaleRequestCode = ambariClient.addHostsAndRackInfoWithBlueprint(blueprintName, hostGroup.getName(), hostsWithRackInfo);
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

}
