package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterAuthenticationContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.cluster.flow.status.AmbariClusterStatusUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Service
public class AmbariClusterFacade implements ClusterFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterFacade.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;
    private static final String ADMIN = "admin";

    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private AmbariStartupListenerTask ambariStartupListenerTask;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private StackService stackService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private ClusterContainerRunner containerRunner;
    @Inject
    private ClusterHostServiceRunner hostRunner;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private AmbariClusterStatusUpdater ambariClusterStatusUpdater;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private InstanceMetadataService instanceMetadataService;
    @Inject
    private ClusterTerminationService clusterTerminationService;
    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    private enum Msg {
        AMBARI_CLUSTER_BUILDING("ambari.cluster.building"),
        AMBARI_CLUSTER_BUILT("ambari.cluster.built"),
        AMBARI_CLUSTER_NOTIFICATION_EMAIL("ambari.cluster.notification.email"),
        AMBARI_CLUSTER_CREATED("ambari.cluster.created"),
        AMBARI_CLUSTER_STARTING("ambari.cluster.starting"),
        AMBARI_CLUSTER_STARTED("ambari.cluster.started"),
        AMBARI_CLUSTER_STOPPING("ambari.cluster.stopping"),
        AMBARI_CLUSTER_STOPPED("ambari.cluster.stopped"),
        AMBARI_CLUSTER_SCALING_UP("ambari.cluster.scaling.up"),
        AMBARI_CLUSTER_SCALED_UP("ambari.cluster.scaled.up"),
        AMBARI_CLUSTER_RESET("ambari.cluster.reset"),
        AMBARI_CLUSTER_RUN_CONTAINERS("ambari.cluster.run.containers"),
        AMBARI_CLUSTER_RUN_SERVICES("ambari.cluster.run.services"),
        AMBARI_CLUSTER_CHANGING_CREDENTIAL("ambari.cluster.changing.credential"),
        AMBARI_CLUSTER_CHANGED_CREDENTIAL("ambari.cluster.changed.credential"),
        AMBARI_CLUSTER_START_REQUESTED("ambari.cluster.start.requested"),
        AMBARI_CLUSTER_START_FAILED("ambari.cluster.start.failed"),
        AMBARI_CLUSTER_STOP_FAILED("ambari.cluster.stop.failed"),
        AMBARI_CLUSTER_CREATE_FAILED("ambari.cluster.create.failed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    @Override
    public FlowContext startAmbari(FlowContext context) throws Exception {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster == null) {
            LOGGER.debug("There is no cluster installed on the stack, skipping start Ambari step");
        } else {
            MDCBuilder.buildMdcContext(cluster);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Ambari cluster is now starting.");
            fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STARTING, UPDATE_IN_PROGRESS.name());
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(clientConfig, stack.getGatewayPort());
            AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, clientConfig.getApiAddress(), ambariClient);
            PollingResult pollingResult = ambariStartupPollerObjectPollingService
                    .pollWithTimeoutSingleFailure(ambariStartupListenerTask, ambariStartupPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            if (isSuccess(pollingResult)) {
                LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
            } else if (isExited(pollingResult)) {
                throw new CancellationException("Polling of Ambari server start has been cancelled.");
            } else {
                LOGGER.info("Could not start Ambari. polling result: {},  Context: {}", pollingResult, context);
                throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s',  Context: '%s'", pollingResult, context));
            }
            changeAmbariCredentials(clientConfig, stack);
        }
        return context;
    }

    @Override
    public FlowContext buildAmbariCluster(FlowContext context) throws Exception {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster == null) {
            LOGGER.debug("There is no cluster installed on the stack, skipping build Ambari step");
        } else {
            MDCBuilder.buildMdcContext(cluster);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, String.format("Building the Ambari cluster. Ambari ip:%s", stack.getAmbariIp()));
            fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_BUILDING, UPDATE_IN_PROGRESS.name(), stack.getAmbariIp());
            cluster = ambariClusterConnector.buildAmbariCluster(stack);
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster creation finished.");
            fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_BUILT, AVAILABLE.name(), stack.getAmbariIp());
            if (cluster.getEmailNeeded()) {
                emailSenderService.sendProvisioningSuccessEmail(cluster.getOwner(), stack.getAmbariIp(), cluster.getName());
                fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
            }
        }
        return context;
    }

    @Override
    public FlowContext startCluster(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster != null && (cluster.isStartRequested())) {
            MDCBuilder.buildMdcContext(cluster);
            clusterService.updateClusterStatusByStackId(stack.getId(), START_IN_PROGRESS);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, String.format("Starting the Ambari cluster. Ambari ip:%s", stack.getAmbariIp()));
            fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STARTING, UPDATE_IN_PROGRESS.name(), stack.getAmbariIp());
            ambariClusterConnector.startCluster(stack);
            cluster.setUpSince(new Date().getTime());
            clusterService.updateCluster(cluster);
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);

            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Ambari cluster started.");
            fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STARTED, AVAILABLE.name(), stack.getAmbariIp());

            if (cluster.getEmailNeeded()) {
                emailSenderService.sendStartSuccessEmail(cluster.getOwner(), stack.getAmbariIp(), cluster.getName());
                fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
            }
        } else {
            LOGGER.info("Cluster start has not been requested, start cluster later.");
        }
        return context;
    }

    @Override
    public FlowContext stopCluster(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        Status stackStatus = stack.getStatus();
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STOPPING, UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_IN_PROGRESS);
        ambariClusterConnector.stopCluster(stack);
        stack = stackService.getById(actualContext.getStackId());
        if (!stackStatus.equals(stack.getStatus())) {
            stackUpdater.updateStackStatus(stack.getId(), stack.isStopRequested() ? STOP_REQUESTED : stackStatus);
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), STOPPED);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STOPPED, STOPPED.name());
        return context;
    }

    @Override
    public FlowContext runClusterContainers(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Orchestrator orchestrator = stack.getOrchestrator();
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(stack);
        if (stack.getCluster() != null && cluster.isRequested()) {
            MDCBuilder.buildMdcContext(cluster);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Running cluster services.");
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
            if (orchestratorType.containerOrchestrator()) {
                fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_RUN_CONTAINERS, UPDATE_IN_PROGRESS.name());
                Map<String, List<Container>> containers = containerRunner.runClusterContainers(actualContext);
                HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack);
                clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
                Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
                for (Map.Entry<String, List<Container>> containersEntry : containers.entrySet()) {
                    List<String> hostNames = new ArrayList<>();
                    for (Container container : containersEntry.getValue()) {
                        hostNames.add(container.getHost());
                    }
                    hostsPerHostGroup.put(containersEntry.getKey(), hostNames);
                }
                clusterService.updateHostMetadata(cluster.getId(), hostsPerHostGroup);
            } else if (orchestratorType.hostOrchestrator()) {
                fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_RUN_SERVICES, UPDATE_IN_PROGRESS.name());
                hostRunner.runAmbariServices(actualContext);
                HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack);
                clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
                Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
                for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                    String groupName = instanceMetaData.getInstanceGroup().getGroupName();
                    if (!hostsPerHostGroup.keySet().contains(groupName)) {
                        hostsPerHostGroup.put(groupName, new ArrayList<>());
                    }
                    hostsPerHostGroup.get(groupName).add(instanceMetaData.getDiscoveryFQDN());
                }
                clusterService.updateHostMetadata(cluster.getId(), hostsPerHostGroup);
            } else {
                LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
                throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
            }
            context = new ProvisioningContext.Builder()
                    .setDefaultParams(stack.getId(), actualContext.getCloudPlatform())
                    .build();
        } else {
            LOGGER.info("The stack has started but there were no cluster request, yet. Won't install cluster now.");
            return actualContext;
        }
        return context;
    }

    @Override
    public FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_RESET, UPDATE_IN_PROGRESS.name());
        ambariClusterConnector.resetAmbariCluster(stack.getId());
        context = new ProvisioningContext.Builder()
                .withProvisioningContext(actualContext)
                .build();
        return context;
    }

    @Override
    public FlowContext startRequested(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_START_REQUESTED, START_REQUESTED.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        return context;
    }

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        ambariClusterStatusUpdater.updateClusterStatus(stack, cluster);
        return context;
    }

    @Override
    public FlowContext credentialChange(FlowContext context) throws CloudbreakException {
        ClusterAuthenticationContext actualContext = (ClusterAuthenticationContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_CHANGING_CREDENTIAL, UPDATE_IN_PROGRESS.name());
        ambariClusterConnector.credentialChangeAmbariCluster(stack.getId(), actualContext.getUser(), actualContext.getPassword());
        clusterService.updateClusterUsernameAndPassword(cluster, actualContext.getUser(), actualContext.getPassword());
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_CHANGED_CREDENTIAL, AVAILABLE.name());
        return actualContext;
    }

    @Override
    public FlowContext handleStartFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), START_FAILED);
        stackUpdater.updateStackStatus(actualContext.getStackId(), AVAILABLE, "Cluster could not be started: " + actualContext.getErrorReason());
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_START_FAILED, AVAILABLE.name(), actualContext.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStartFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), cluster.getName());
            fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, START_FAILED.name());
        }
        return context;
    }

    @Override
    public FlowContext handleStopFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "The Ambari cluster could not be stopped: " + actualContext.getErrorReason());
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_STOP_FAILED, AVAILABLE.name(), actualContext.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStopFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), cluster.getName());
            fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, STOP_FAILED.name());
        }
        return context;
    }

    @Override
    public FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException {
        return handleClusterCreationFailure(context, true);
    }

    @Override
    public FlowContext handleClusterInstallationFailure(FlowContext context) throws CloudbreakException {
        return handleClusterCreationFailure(context, false);
    }

    private FlowContext handleClusterCreationFailure(FlowContext context, boolean deleteClusterContainers) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), CREATE_FAILED, actualContext.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        fireEventAndLog(stack.getId(), context, Msg.AMBARI_CLUSTER_CREATE_FAILED, CREATE_FAILED.name(), actualContext.getErrorReason());

        if (deleteClusterContainers) {
            try {
                clusterTerminationService.deleteClusterContainers(cluster.getId());
            } catch (TerminationFailedException ex) {
                LOGGER.error("Cluster containers could not be deleted, preparation for reinstall failed: ", ex);
            }
        }

        if (cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningFailureEmail(cluster.getOwner(), cluster.getName());
            fireEventAndLog(actualContext.getStackId(), context, Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
        return actualContext;
    }

    private HttpClientConfig buildAmbariClientConfig(Stack stack) throws CloudbreakSecuritySetupException {
        Map<InstanceGroupType, InstanceStatus> newStatusByGroupType = new HashMap<>();
        newStatusByGroupType.put(InstanceGroupType.GATEWAY, InstanceStatus.REGISTERED);
        newStatusByGroupType.put(InstanceGroupType.CORE, InstanceStatus.UNREGISTERED);
        instanceMetadataService.updateInstanceStatus(stack.getInstanceGroups(), newStatusByGroupType);
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        return tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
    }

    private void changeAmbariCredentials(HttpClientConfig ambariClientConfig, Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        LOGGER.info("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.getName(), ambariClientConfig.getApiAddress());
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(ambariClientConfig, stack.getGatewayPort());
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariClient.changePassword(ADMIN, ADMIN, password, true);
            }
        } else {
            ambariClient.createUser(userName, password, true);
            ambariClient.deleteUser(ADMIN);
        }
    }

    private void fireEventAndLog(Long stackId, FlowContext context, Msg msgCode, String eventType, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", msgCode, context);
        eventService.fireCloudbreakEvent(stackId, eventType, messagesService.getMessage(msgCode.code(), Arrays.asList(args)));
    }
}
