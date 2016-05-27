package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterAuthenticationContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
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
        AMBARI_CLUSTER_CREATED("ambari.cluster.created"),
        AMBARI_CLUSTER_STARTING("ambari.cluster.starting"),
        AMBARI_CLUSTER_STARTED("ambari.cluster.started"),
        AMBARI_CLUSTER_STOPPING("ambari.cluster.stopping"),
        AMBARI_CLUSTER_STOPPED("ambari.cluster.stopped"),
        AMBARI_CLUSTER_SCALING_UP("ambari.cluster.scaling.up"),
        AMBARI_CLUSTER_SCALED_UP("ambari.cluster.scaled.up"),
        AMBARI_CLUSTER_RESET("ambari.cluster.reset"),
        AMBARI_CLUSTER_CHANGING_CREDENTIAL("ambari.cluster.changing.credential"),
        AMBARI_CLUSTER_CHANGED_CREDENTIAL("ambari.cluster.changed.credential"),
        AMBARI_CLUSTER_START_FAILED("ambari.cluster.start.failed"),
        AMBARI_CLUSTER_STOP_FAILED("ambari.cluster.stop.failed"),
        AMBARI_CLUSTER_CREATE_FAILED("ambari.cluster.create.failed"),
        AMBARI_CLUSTER_NOTIFICATION_EMAIL("ambari.cluster.notification.email");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
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

    private void fireEventAndLog(Long stackId, FlowContext context, Msg msgCode, String eventType, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", msgCode, context);
        eventService.fireCloudbreakEvent(stackId, eventType, messagesService.getMessage(msgCode.code(), Arrays.asList(args)));
    }
}
