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
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.StackUpdaterService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationState;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.CheckFreeIpaExistsEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.PollBindUserCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.StartBindUserCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.ValidateKerberosConfigEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

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

    @Bean(name = "CHECK_FREEIPA_EXISTS_STATE")
    public Action<?, ?> checkFreeIpaExistsAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                CheckFreeIpaExistsEvent event = new CheckFreeIpaExistsEvent(payload.getResourceId());
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CREATE_BIND_USER_STATE")
    public Action<?, ?> createBindUserAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                StartBindUserCreationEvent event = new StartBindUserCreationEvent(payload.getResourceId());
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "POLL_BIND_USER_CREATION_STATE")
    public Action<?, ?> pollBindUserCreationAction() {
        return new AbstractStackCreationAction<>(PollBindUserCreationEvent.class) {

            @Override
            protected void doExecute(StackContext context, PollBindUserCreationEvent payload, Map<Object, Object> variables) {
                PollBindUserCreationEvent event = new PollBindUserCreationEvent(payload.getResourceId(), payload.getOperationId());
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(PollBindUserCreationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "VALIDATE_KERBEROS_CONFIG_STATE")
    public Action<?, ?> kerberosConfigValidationAction() {
        return new AbstractStackCreationAction<>(ValidateKerberosConfigEvent.class) {
            @Override
            protected void doExecute(StackContext context, ValidateKerberosConfigEvent payload, Map<Object, Object> variables) {
                decorateStackWithCustomDomainIfAdOrIpaJoinable(context.getStack());
                Cluster cluster = context.getStack().getCluster();
                if ((cluster != null && Boolean.TRUE.equals(cluster.getAutoTlsEnabled())) || payload.doesFreeipaExistsForEnv()) {
                    boolean hasFreeIpaKerberosConfig = clusterCreationEnvironmentValidator.hasFreeIpaKerberosConfig(context.getStack());
                    if (!hasFreeIpaKerberosConfig) {
                        throw new IllegalStateException("AutoTLS works only with FreeIPA. No FreeIPA Kerberos configuration is found.");
                    }
                }
                sendEvent(context, KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FINISHED_EVENT.selector(), payload);
            }

            @Override
            protected Object getFailurePayload(ValidateKerberosConfigEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new StackFailureEvent(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.selector(), payload.getResourceId(), ex);
            }

            private void decorateStackWithCustomDomainIfAdOrIpaJoinable(Stack stack) {
                Optional<KerberosConfig> kerberosConfig = measure(() -> kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()),
                        LOGGER, "kerberosConfigService get {} ms");
                if (kerberosConfig.isPresent() && StringUtils.isNotBlank(kerberosConfig.get().getDomain())) {
                    LOGGER.info("Setting custom domain [{}] for cluster [{}]", kerberosConfig.get().getDomain(), stack.getName());
                    stack.setCustomDomain(kerberosConfig.get().getDomain());
                    stackService.save(stack);
                } else {
                    LOGGER.info("No kerberos config or no  custom domain found");
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
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
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
