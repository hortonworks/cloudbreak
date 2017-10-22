package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterMinimalContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterUpgradeResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Configuration
public class ClusterUpgradeActions {

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterUpgradeService clusterUpgradeService;

    @Bean(name = "CLUSTER_UPGRADE_STATE")
    public Action upgradeCluster() {
        return new AbstractClusterUpgradeAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterMinimalContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                clusterUpgradeService.upgradeCluster(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterMinimalContext context) {
                return new ClusterUpgradeRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_FINISHED_STATE")
    public Action clusterUpgradeFinished() {
        return new AbstractClusterAction<ClusterUpgradeResult>(ClusterUpgradeResult.class) {
            @Override
            protected void doExecute(ClusterMinimalContext context, ClusterUpgradeResult payload, Map<Object, Object> variables) throws Exception {
                clusterUpgradeService.clusterUpgradeFinished(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterMinimalContext context) {
                return new StackEvent(ClusterUpgradeEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_FAILED_STATE")
    public Action clusterStartFailedAction() {
        return new AbstractStackFailureAction<ClusterUpgradeState, ClusterUpgradeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterUpgradeService.handleUpgradeClusterFailure(context.getStackMinimal().getId(), payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterUpgradeEvent.FAIL_HANDLED_EVENT.event(), context.getStackMinimal().getId());
            }
        };
    }

}
