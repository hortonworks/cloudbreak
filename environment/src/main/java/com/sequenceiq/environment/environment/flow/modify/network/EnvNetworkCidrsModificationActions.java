package com.sequenceiq.environment.environment.flow.modify.network;

import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINALIZE_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvNetworkCidrsModificationActions {
    private static final Logger LOGGER = getLogger(EnvNetworkCidrsModificationActions.class);

    @Inject
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Bean(name = "ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE")
    public Action<?, ?> initNetworkCidrsModificationOnEnvironment() {
        return new AbstractEnvNetworkCidrsModificationAction<>(EnvNetworkCidrsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvNetworkCidrsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify network CIDRs on environment state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_STARTED,
                        EnvNetworkCidrsModificationState.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_START_STATE);
                EnvNetworkCidrsModificationEvent event = EnvNetworkCidrsModificationEvent.builder()
                        .withSelector(START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT.event())
                        .withResourceId(payload.getResourceId())
                        .withResourceName(payload.getResourceName())
                        .withResourceCrn(payload.getResourceCrn())
                        .withNetworkCidrs(payload.getNetworkCidrs())
                        .build();
                sendEvent(context, START_MODIFY_NETWORK_CIDRS_FREEIPA_EVENT.event(), event);
            }

            @Override
            protected Object getFailurePayload(EnvNetworkCidrsModificationEvent payload, Optional<CommonContext> context, Exception ex) {
                return new EnvNetworkCidrsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }
        };
    }

    @Bean(name = "NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE")
    public Action<?, ?> modifyNetworkCidrsOnFreeIpa() {
        return new AbstractEnvNetworkCidrsModificationAction<>(EnvNetworkCidrsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvNetworkCidrsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify network CIDRs on FreeIPA state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_STARTED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FREEIPA_STATE);
                EnvNetworkCidrsModificationEvent event = EnvNetworkCidrsModificationEvent.builder()
                        .withSelector(MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT.event())
                        .withResourceId(payload.getResourceId())
                        .withResourceName(payload.getResourceName())
                        .withResourceCrn(payload.getResourceCrn())
                        .withNetworkCidrs(payload.getNetworkCidrs())
                        .build();
                sendEvent(context, MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT.event(), event);
            }

            @Override
            protected Object getFailurePayload(EnvNetworkCidrsModificationEvent payload, Optional<CommonContext> context, Exception ex) {
                return new EnvNetworkCidrsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_FREEIPA_FAILED;
            }
        };
    }

    @Bean(name = "NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE")
    public Action<?, ?> modifyNetworkCidrsOnDatalakeAndDataHubs() {
        return new AbstractEnvNetworkCidrsModificationAction<>(EnvNetworkCidrsModificationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvNetworkCidrsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify network CIDRs on Data Lake and Data Hubs state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_STARTED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_DATALAKE_AND_DATAHUBS_STATE);
                EnvNetworkCidrsModificationEvent event = EnvNetworkCidrsModificationEvent.builder()
                        .withSelector(MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT.event())
                        .withResourceId(payload.getResourceId())
                        .withResourceName(payload.getResourceName())
                        .withResourceCrn(payload.getResourceCrn())
                        .withNetworkCidrs(payload.getNetworkCidrs())
                        .build();
                sendEvent(context, MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT.event(), event);
            }

            @Override
            protected Object getFailurePayload(EnvNetworkCidrsModificationEvent payload, Optional<CommonContext> context, Exception ex) {
                return new EnvNetworkCidrsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.NETWORK_CIDRS_MODIFICATION_ON_DATALAKE_AND_DATAHUBS_FAILED;
            }
        };
    }

    @Bean(name = "NETWORK_CIDRS_MODIFICATION_FINISHED_STATE")
    public Action<?, ?> modifyNetworkCidrsFinished() {
        return new AbstractEnvNetworkCidrsModificationAction<>(EnvNetworkCidrsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvNetworkCidrsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify network CIDRs finished state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FINISHED,
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FINISHED_STATE);
                EnvNetworkCidrsModificationEvent event = EnvNetworkCidrsModificationEvent.builder()
                        .withSelector(FINALIZE_MODIFY_NETWORK_CIDRS_EVENT.event())
                        .withResourceId(payload.getResourceId())
                        .withResourceName(payload.getResourceName())
                        .withResourceCrn(payload.getResourceCrn())
                        .withNetworkCidrs(payload.getNetworkCidrs())
                        .build();
                sendEvent(context, FINALIZE_MODIFY_NETWORK_CIDRS_EVENT.event(), event);
            }
        };
    }

    @Bean(name = "NETWORK_CIDRS_MODIFICATION_FAILED_STATE")
    public Action<?, ?> modifyNetworkCidrsFailed() {
        return new AbstractEnvNetworkCidrsModificationAction<>(EnvNetworkCidrsModificationFailureEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvNetworkCidrsModificationFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Modifying network CIDRs failed: {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_NETWORK_CIDRS_MODIFICATION_FAILED,
                        List.of(payload.getEnvironmentStatus(), payload.getException().getMessage()),
                        EnvNetworkCidrsModificationState.NETWORK_CIDRS_MODIFICATION_FAILED_STATE);
                sendEvent(context, HANDLED_FAILED_MODIFY_NETWORK_CIDRS_EVENT.event(), payload);
            }
        };
    }
}
