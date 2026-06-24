package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup;

import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent.SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SECURITY_GROUP_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SECURITY_GROUP_VALIDATION_FINISHED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.SecurityGroupValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config.SecurityGroupValidationState;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;

@Configuration
public class SecurityGroupValidationActions {

    private static final Logger LOGGER = getLogger(SecurityGroupValidationActions.class);

    private static final String VALIDATION_IN_PROGRESS_REASON = "Validating security groups";

    private static final String VALIDATION_FINISHED_REASON = "Security group validation finished";

    private static final String MISSING_SECURITY_GROUPS_ERROR = "Operation cannot start: security group(s) %s referenced in "
            + "cluster metadata do not exist. Update security group metadata or restore the groups before retrying.";

    private static final String VPC_MISMATCH_ERROR = "Operation cannot start: security group(s) %s referenced in cluster metadata "
            + "do not belong to the environment VPC. Update security group metadata before retrying.";

    @Inject
    private AwsSecurityGroupValidationRequestProvider requestProvider;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "SECURITY_GROUP_VALIDATION_STATE")
    public Action<?, ?> validateSecurityGroups() {
        return new AbstractSecurityGroupValidationAction<>(SecurityGroupValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, SecurityGroupValidationTriggerEvent payload, Map<Object, Object> variables)
                    throws Exception {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.SECURITY_GROUP_VALIDATION_STARTED,
                        VALIDATION_IN_PROGRESS_REASON);
                Set<String> securityGroupIds = requestProvider.collectSecurityGroupIds(context.getStack());
                if (securityGroupIds.isEmpty()) {
                    LOGGER.debug("No security group ids to validate for stack {}, short-circuiting flow", context.getStack().getId());
                    SecurityGroupValidationResult emptyResult = new SecurityGroupValidationResult(payload.getResourceId(), Set.of(), Set.of());
                    sendEvent(context, SECURITY_GROUP_VALIDATION_FINISHED_EVENT.event(), emptyResult);
                } else {
                    String vpcId = requestProvider.resolveAwsVpcId(context.getStack().getEnvironmentCrn());
                    ExtendedCloudCredential extendedCloudCredential = credentialClientService.getExtendedCloudCredential(context.getStack().getEnvironmentCrn());
                    SecurityGroupValidationRequest request = new SecurityGroupValidationRequest(
                            context.getCloudContext(),
                            context.getCloudCredential(),
                            extendedCloudCredential,
                            context.getCloudContext().getLocation().getRegion().getRegionName(),
                            securityGroupIds,
                            vpcId);
                    sendEvent(context, request.selector(), request);
                }
            }
        };
    }

    @Bean(name = "SECURITY_GROUP_VALIDATION_RESULT_STATE")
    public Action<?, ?> handleValidationResult() {
        return new AbstractSecurityGroupValidationAction<>(SecurityGroupValidationResult.class) {

            @Override
            protected void doExecute(StackContext context, SecurityGroupValidationResult payload, Map<Object, Object> variables) throws Exception {
                if (payload.getStatus() == EventStatus.FAILED) {
                    sendValidationFailure(context, payload.getResourceId(), payload.getStatusReason() != null
                            ? payload.getStatusReason()
                            : "Security group validation failed");
                } else {
                    Set<String> missing = payload.getMissingSecurityGroupIds();
                    Set<String> notInNetwork = payload.getNotInNetworkSecurityGroupIds();
                    if (!missing.isEmpty()) {
                        LOGGER.warn("Security group validation failed for stack {} due to missing security groups: {}",
                                context.getStack().getId(), missing);
                        sendValidationFailure(context, payload.getResourceId(),
                                String.format(MISSING_SECURITY_GROUPS_ERROR, formatSecurityGroupIds(missing)));
                    } else if (!notInNetwork.isEmpty()) {
                        LOGGER.warn("Security group validation failed for stack {} due to VPC mismatch for security groups: {}",
                                context.getStack().getId(), notInNetwork);
                        sendValidationFailure(context, payload.getResourceId(),
                                String.format(VPC_MISMATCH_ERROR, formatSecurityGroupIds(notInNetwork)));
                    } else {
                        Long stackId = payload.getResourceId();
                        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.REPAIR_IN_PROGRESS, VALIDATION_FINISHED_REASON);
                        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), STACK_SECURITY_GROUP_VALIDATION_FINISHED);
                        sendEvent(context, SECURITY_GROUP_VALIDATION_FINALIZED_EVENT.event(),
                                new StackEvent(SECURITY_GROUP_VALIDATION_FINALIZED_EVENT.event(), stackId));
                    }
                }
            }

            private String formatSecurityGroupIds(Set<String> securityGroupIds) {
                return securityGroupIds.stream().sorted().collect(Collectors.joining(", ", "[", "]"));
            }
        };
    }

    @Bean(name = "SECURITY_GROUP_VALIDATION_FAILED_STATE")
    public Action<?, ?> validationFailed() {
        return new AbstractStackFailureAction<SecurityGroupValidationState, SecurityGroupValidationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                String errorReason = payload.getException().getMessage();
                Long stackId = payload.getResourceId();
                LOGGER.warn("Security group validation failed: {}", errorReason, payload.getException());
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.SECURITY_GROUP_VALIDATION_FAILED, errorReason);
                flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), STACK_SECURITY_GROUP_VALIDATION_FAILED, errorReason);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
