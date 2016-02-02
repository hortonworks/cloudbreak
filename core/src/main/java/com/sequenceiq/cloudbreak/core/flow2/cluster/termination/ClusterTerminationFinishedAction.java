package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("ClusterTerminationFinishedAction")
public class ClusterTerminationFinishedAction extends AbstractClusterTerminationAction<TerminateClusterResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFinishedAction.class);

    @Inject
    private ClusterTerminationService terminationService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private ClusterService clusterService;

    protected ClusterTerminationFinishedAction() {
        super(TerminateClusterResult.class);
    }

    @Override
    protected void doExecute(ClusterContext context, TerminateClusterResult payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Terminate cluster result: {}", payload);
        Cluster cluster = context.getCluster();
        terminationService.finalizeClusterTermination(cluster.getId());
        cloudbreakEventService.fireCloudbreakEvent(cluster.getStack().getId(), DELETE_COMPLETED.name(),
                messagesService.getMessage(Msg.CLUSTER_DELETE_COMPLETED.code(), Collections.singletonList(cluster.getId())));
        clusterService.updateClusterStatusByStackId(cluster.getStack().getId(), DELETE_COMPLETED);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendTerminationSuccessEmail(cluster.getOwner(), cluster.getAmbariIp(), cluster.getName());
            cloudbreakEventService.fireCloudbreakEvent(cluster.getStack().getId(), DELETE_COMPLETED.name(),
                    messagesService.getMessage(Msg.CLUSTER_EMAIL_SENT.code()));
        }
        sendEvent(context.getFlowId(), ClusterTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), null);
    }

    @Override
    protected Long getClusterId(TerminateClusterResult payload) {
        return payload.getRequest().getClusterContext().getClusterId();
    }

}
