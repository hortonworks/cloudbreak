package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationCheckerTask;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationPollerObject;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Configuration
public class KerberosConfigValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigValidationActions.class);

    @Inject
    private ClusterCreationEnvironmentValidator clusterCreationEnvironmentValidator;

    @Inject
    private StackUpdaterService stackUpdaterService;

    @Inject
    private StackService stackService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private OperationV1Endpoint operationV1Endpoint;

    @Inject
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @Bean(name = "VALIDATE_KERBEROS_CONFIG_STATE")
    public Action<?, ?> kerberosConfigValidationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                BindUserCreateRequest request = new BindUserCreateRequest();
                request.setEnvironmentCrn(context.getStack().getEnvironmentCrn());
                request.setBindUserNameSuffix(context.getStack().getName());
                OperationStatus operation = freeipaClientService.createBindUsers(request, ThreadBasedUserCrnProvider.getUserCrn());
                FreeIpaOperationPollerObject operationPollerObject = new FreeIpaOperationPollerObject(operation.getOperationId(),
                        operation.getOperationType().name(), operationV1Endpoint);
                freeIpaOperationChecker.pollWithAbsoluteTimeoutSingleFailure(new FreeIpaOperationCheckerTask<>(), operationPollerObject, 5000, 600L);
                decorateStackWithCustomDomainIfAdOrIpaJoinable(context.getStack());
                Cluster cluster = context.getStack().getCluster();
                if (cluster != null && Boolean.TRUE.equals(cluster.getAutoTlsEnabled())) {
                    boolean hasFreeIpaKerberosConfig = clusterCreationEnvironmentValidator.hasFreeIpaKerberosConfig(context.getStack());
                    if (!hasFreeIpaKerberosConfig) {
                        throw new IllegalStateException("Kerberos config validation failed on freeipa");
                    }
                }
                sendEvent(context, KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FINISHED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }

            private void decorateStackWithCustomDomainIfAdOrIpaJoinable(Stack stack) {
                KerberosConfig kerberosConfig = measure(() -> kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null),
                        LOGGER,
                        "kerberosConfigService get {} ms");
                if (kerberosConfig != null && StringUtils.isNotBlank(kerberosConfig.getDomain())) {
                    stack.setCustomDomain(kerberosConfig.getDomain());
                    stackService.save(stack);
                }
            }
        };
    }

    @Bean(name = "VALIDATE_KERBEROS_CONFIG_FAILED_STATE")
    public Action<?, ?> kerberosConfigValidationFailureAction() {
        return new AbstractStackFailureAction<KerberosConfigValidationState, KerberosConfigValidationEvent>() {

            @Override
            protected StackFailureContext createFlowContext(FlowParameters flowParameters,
                    StateContext<KerberosConfigValidationState, KerberosConfigValidationEvent> stateContext, StackFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                StackView stackView = stackService.getViewByIdWithoutAuth(payload.getResourceId());
                MDCBuilder.buildMdcContext(stackView);
                flow.setFlowFailed(payload.getException());
                return new StackFailureContext(flowParameters, stackView);
            }

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                stackUpdaterService.updateStatusAndSendEventWithArgs(context.getStackView().getId(), DetailedStackStatus.PROVISION_FAILED,
                        ResourceEvent.KERBEROS_CONFIG_VALIDATION_FAILED, payload.getException().getMessage(), payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILURE_HANDLED_EVENT.selector(), context.getStackView().getId());
            }
        };
    }
}
