package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("ClusterTerminationFailureAction")
public class ClusterTerminationFailureAction extends AbstractClusterTerminationAction<TerminateClusterResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFailureAction.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private EmailSenderService emailSenderService;

    protected ClusterTerminationFailureAction() {
        super(TerminateClusterResult.class);
    }

    @Override
    protected void doExecute(ClusterContext context, TerminateClusterResult payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Handling cluster delete failure event.");
        Msg eventMessage = Msg.CLUSTER_DELETE_FAILED;
        Exception errorDetails = payload.getErrorDetails();
        LOGGER.error("Error during cluster termination flow: ", errorDetails);
        Cluster cluster = clusterService.getById(context.getCluster().getId());
        cluster.setStatus(DELETE_FAILED);
        cluster.setStatusReason(errorDetails.getMessage());
        clusterService.updateCluster(cluster);
        String message = messagesService.getMessage(eventMessage.code(), Arrays.asList(errorDetails.getMessage()));
        eventService.fireCloudbreakEvent(cluster.getStack().getId(), DELETE_FAILED.name(), message);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendTerminationFailureEmail(cluster.getOwner(), cluster.getAmbariIp(), cluster.getName());
            eventService.fireCloudbreakEvent(cluster.getStack().getId(), DELETE_FAILED.name(),
                    messagesService.getMessage(Msg.CLUSTER_EMAIL_SENT.code()));
        }
    }

    @Override
    protected Long getClusterId(TerminateClusterResult payload) {
        return payload.getRequest().getClusterContext().getClusterId();
    }
}
