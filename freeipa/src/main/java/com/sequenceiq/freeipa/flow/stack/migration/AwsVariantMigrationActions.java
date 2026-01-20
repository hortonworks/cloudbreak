package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FINALIZED_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.migration.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.metrics.MetricType;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class AwsVariantMigrationActions {

    private static final Logger LOGGER = getLogger(AwsVariantMigrationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FreeIpaMetricService metricService;

    @Bean(name = "CREATE_RESOURCES_STATE")
    public Action<?, ?> createResources() {
        return new AbstractAwsVariantMigrationAction<>(AwsVariantMigrationTriggerEvent.class) {

            @Override
            protected void doExecute(AwsVariantMigrationFlowContext context, AwsVariantMigrationTriggerEvent payload, Map<Object, Object> variables)
                    throws Exception {
                CreateResourcesRequest request = new CreateResourcesRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        payload.getHostGroupName());
                sendEvent(context, request.selector(), request);
            }

        };
    }

    @Bean(name = "DELETE_CLOUD_FORMATION_STATE")
    public Action<?, ?> deleteCloudFormation() {
        return new AbstractAwsVariantMigrationAction<>(CreateResourcesResult.class) {
            @Override
            protected void doExecute(AwsVariantMigrationFlowContext context, CreateResourcesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(AwsVariantMigrationFlowContext context) {
                return new DeleteCloudFormationRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "CHANGE_VARIANT_STATE")
    public Action<?, ?> changeVariant() {
        return new AbstractAwsVariantMigrationAction<>(DeleteCloudFormationResult.class) {
            @Override
            protected void doExecute(AwsVariantMigrationFlowContext context, DeleteCloudFormationResult payload, Map<Object, Object> variables)
                    throws Exception {
                if (payload.isCloudFormationTemplateDeleted()) {
                    LOGGER.debug("Variant will be changed");
                    stackUpdater.updateVariant(payload.getResourceId(), CloudConstants.AWS_NATIVE);
                    LOGGER.info("Variant changed to AWS_NATIVE");
                    metricService.incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_SUCCESSFUL, context.getStack());
                } else {
                    LOGGER.info("Variant won't be changed because the CF template exists");
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(AwsVariantMigrationFlowContext context) {
                return new StackEvent(AWS_VARIANT_MIGRATION_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "AWS_VARIANT_MIGRATION_FAILED_STATE")
    public Action<?, ?> migrationFailed() {
        return new AbstractAwsVariantMigrationAction<>(StackFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(AwsVariantMigrationFlowContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                String errorReason = payload.getException().getMessage();
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                String message = String.format("Aws variant migration failed: %s", errorReason);
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                LOGGER.info(message, payload.getException());
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPGRADE_FAILED, "AWS variant migration failed. " + errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                metricService.incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_FAILED, stack, payload.getException());
                sendEvent(context, AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}