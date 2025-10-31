package com.sequenceiq.cloudbreak.core.flow2.cluster.update.publicdns;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns.UpdatePublicDnsEntriesInPemRequest;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.Flow;

@Configuration
public class UpdatePublicDnsEntriesFlowActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePublicDnsEntriesFlowActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "UPDATE_PUBLIC_DNS_ENTRIES_IN_PEM_STATE")
    public Action<?, ?> updatePublicDnsEntriesInPemAction() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting to update public DNS entries in PEM.");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.UPDATE_PUBLIC_DNS_ENTRIES,
                        "Updating cluster's public DNS entries in PEM.");
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES);
                UpdatePublicDnsEntriesInPemRequest updatePublicDnsEntriesInPemRequest = new UpdatePublicDnsEntriesInPemRequest(payload.getResourceId());
                sendEvent(context, updatePublicDnsEntriesInPemRequest);
            }
        };
    }

    @Bean(name = "UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_STATE")
    public Action<?, ?> updatePublicDnsEntriesInPemFinishedAction() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Finished the update of public DNS entries in PEM.");
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE,
                        "Cluster's public DNS entries have been updated successfully in PEM.");
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES_FINISHED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPDATE_PUBLIC_DNS_ENTRIES_FAILED_STATE")
    public Action<?, ?> updatePublicDnsEntriesInPemFailedAction() {
        return new AbstractStackFailureAction<UpdatePublicDnsEntriesFlowState, UpdatePublicDnsEntriesFlowEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Update of public DNS entries failed: {}", payload.getException().getMessage(), payload.getException());
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.UPDATE_PUBLIC_DNS_ENTRIES_FAILED);
                flowMessageService.fireEventAndLog(payload.getResourceId(), AVAILABLE.name(), CLUSTER_UPDATE_PUBLIC_DNS_ENTRIES_FAILED,
                        payload.getException().getMessage());
                Flow flow = getFlow(context.getFlowId());
                flow.setFlowFailed(payload.getException());
                sendEvent(context, UpdatePublicDnsEntriesFlowEvent.UPDATE_PUBLIC_DNS_ENTRIES_FAILURE_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
