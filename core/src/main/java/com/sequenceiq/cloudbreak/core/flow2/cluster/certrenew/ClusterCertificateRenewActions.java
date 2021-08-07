package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeployRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateRedeploySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew.ClusterCertificateReissueSuccess;

@Configuration
public class ClusterCertificateRenewActions {

    @Inject
    private ClusterCertificateRenewService certificateRenewService;

    @Bean(name = "CLUSTER_CERTIFICATE_REISSUE_STATE")
    public Action<?, ?> clusterCertificateReissueAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                certificateRenewService.reissueCertificate(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterCertificateReissueRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATE_REDEPLOY_STATE")
    public Action<?, ?> clusterCertificateRedeployAction() {
        return new AbstractStackCreationAction<>(ClusterCertificateReissueSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterCertificateReissueSuccess payload, Map<Object, Object> variables) {
                certificateRenewService.redeployCertificateOnCluster(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterCertificateRedeployRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE")
    public Action<?, ?> clusterCertificateRenewalFinishedAction() {
        return new AbstractStackCreationAction<>(ClusterCertificateRedeploySuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterCertificateRedeploySuccess payload, Map<Object, Object> variables) {
                certificateRenewService.certificateRenewalFinished(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_RENEW_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATE_RENEW_FAILED_STATE")
    public Action<?, ?> clusterCreationFailedAction() {
        return new AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                certificateRenewService.certificateRenewalFailed(context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}