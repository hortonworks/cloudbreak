package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.RotateRdsCertificateEvent.FINALIZED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RollingRestartServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RollingRestartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.UpdateLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class RotateRdsCertificateActions {

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Bean(name = "ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_STATE")
    public Action<?, ?> checkPrerequisites() {
        return new AbstractRotateRdsCertificateAction<>(RotateRdsCertificateTriggerRequest.class) {

            @Override
            protected void doExecute(RotateRdsCertificateContext context, RotateRdsCertificateTriggerRequest payload,
                    Map<Object, Object> variables) {
                rotateRdsCertificateService.checkPrerequisitesState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new RotateRdsCertificateCheckPrerequisitesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_GET_LATEST_STATE")
    public Action<?, ?> getLatestRdsCertificate() {
        return new AbstractRotateRdsCertificateAction<>(RotateRdsCertificateCheckPrerequisitesResult.class) {

            @Override
            protected void doExecute(RotateRdsCertificateContext context, RotateRdsCertificateCheckPrerequisitesResult payload,
                    Map<Object, Object> variables) {
                rotateRdsCertificateService.getLatestRdsCertificateState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new GetLatestRdsCertificateRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_UPDATE_TO_LATEST_STATE")
    public Action<?, ?> updateLatestRdsCertificate() {
        return new AbstractRotateRdsCertificateAction<>(GetLatestRdsCertificateResult.class) {
            @Override
            protected void doExecute(RotateRdsCertificateContext context, GetLatestRdsCertificateResult payload, Map<Object, Object> variables) {
                rotateRdsCertificateService.updateLatestRdsCertificateState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new UpdateLatestRdsCertificateRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_RESTART_CM_STATE")
    public Action<?, ?> restartCmService() {
        return new AbstractRotateRdsCertificateAction<>(UpdateLatestRdsCertificateResult.class) {
            @Override
            protected void doExecute(RotateRdsCertificateContext context, UpdateLatestRdsCertificateResult payload, Map<Object, Object> variables) {
                rotateRdsCertificateService.restartCmState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new RestartCmRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_ROLLING_RESTART_STATE")
    public Action<?, ?> rollingRestartRdsCertificate() {
        return new AbstractRotateRdsCertificateAction<>(RestartCmResult.class) {
            @Override
            protected void doExecute(RotateRdsCertificateContext context, RestartCmResult payload, Map<Object, Object> variables) {
                rotateRdsCertificateService.rollingRestartRdsCertificateState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new RollingRestartServicesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_ON_PROVIDER_STATE")
    public Action<?, ?> rotateRdsCertOnProvider() {
        return new AbstractRotateRdsCertificateAction<>(RollingRestartServicesResult.class) {
            @Override
            protected void doExecute(RotateRdsCertificateContext context, RollingRestartServicesResult payload, Map<Object, Object> variables) {
                rotateRdsCertificateService.rotateOnProviderState(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new RotateRdsCertificateOnProviderRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_FINISHED_STATE")
    public Action<?, ?> rotateRdsCertFinished() {
        return new AbstractRotateRdsCertificateAction<>(RotateRdsCertificateOnProviderResult.class) {
            @Override
            protected void doExecute(RotateRdsCertificateContext context, RotateRdsCertificateOnProviderResult payload, Map<Object, Object> variables) {
                getMetricService().incrementMetricCounter(MetricType.ROTATE_RDS_CERTIFICATE_SUCCESSFUL, context.getStack());
                rotateRdsCertificateService.rotateRdsCertFinished(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RotateRdsCertificateContext context) {
                return new StackEvent(FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "ROTATE_RDS_CERTIFICATE_FAILED_STATE")
    public Action<?, ?> rotateRdsCertFailed() {
        return new AbstractStackFailureAction<RotateRdsCertificateState, RotateRdsCertificateEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                RotateRdsCertificateFailedEvent concretePayload = (RotateRdsCertificateFailedEvent) payload;
                rotateRdsCertificateService.rotateRdsCertFailed(concretePayload);
                getMetricService().incrementMetricCounter(MetricType.ROTATE_RDS_CERTIFICATE_FAILED, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(RotateRdsCertificateEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
