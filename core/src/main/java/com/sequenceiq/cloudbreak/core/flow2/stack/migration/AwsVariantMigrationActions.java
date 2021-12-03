package com.sequenceiq.cloudbreak.core.flow2.stack.migration;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent.AWS_VARIANT_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_UPDATE_FAILED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class AwsVariantMigrationActions {

    private static final Logger LOGGER = getLogger(AwsVariantMigrationActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

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
                    getMetricService().incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_SUCCESSFUL, context.getStack());
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
        return new AbstractStackFailureAction<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                String errorReason = payload.getException().getMessage();
                Long stackId = payload.getResourceId();
                LOGGER.info("Aws variant migration failed: {}", errorReason, payload.getException());
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_UPGRADE_FAILED, "AWS variant migration failed. " + errorReason);
                flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_INFRASTRUCTURE_UPDATE_FAILED, errorReason);
                getMetricService().incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

}
