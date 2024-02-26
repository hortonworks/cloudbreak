package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_FAILED;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Configuration
public class CoreVerticalScaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreVerticalScaleActions.class);

    private static final String PREVIOUS_INSTANCE_TYPE = "PREVIOUS_INSTANCE_TYPE";

    private static final String TARGET_INSTANCE_TYPE = "TARGET_INSTANCE_TYPE";

    private static final String GROUP_BEING_SCALED = "GROUP_BEING_SCALED";

    @Inject
    private CoreVerticalScaleService coreVerticalScaleService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Bean(name = "STACK_VERTICALSCALE_STATE")
    public Action<?, ?> stackVerticalScale() {
        return new AbstractClusterAction<>(CoreVerticalScalingTriggerEvent.class) {
            @Override
            protected void prepareExecution(CoreVerticalScalingTriggerEvent payload, Map<Object, Object> variables) {
                Optional<InstanceGroupView> optionalGroup =
                        instanceGroupService.findInstanceGroupViewByStackIdAndGroupName(payload.getRequest().getStackId(), payload.getRequest().getGroup());
                String previousInstanceType = optionalGroup.map(InstanceGroupView::getTemplate).map(Template::getInstanceType).orElse("unknown");
                variables.put(PREVIOUS_INSTANCE_TYPE, previousInstanceType);
                if (payload.getRequest().getTemplate().getInstanceType() != null) {
                    variables.put(TARGET_INSTANCE_TYPE, payload.getRequest().getTemplate().getInstanceType());
                } else {
                    variables.put(TARGET_INSTANCE_TYPE, previousInstanceType);
                }
                variables.put(GROUP_BEING_SCALED, payload.getRequest().getGroup());
            }

            @Override
            protected void doExecute(ClusterViewContext ctx, CoreVerticalScalingTriggerEvent payload, Map<Object, Object> variables) {
                StackVerticalScaleV4Request stackVerticalScaleV4Request = payload.getRequest();
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                coreVerticalScaleService.verticalScale(ctx.getStackId(), stackVerticalScaleV4Request, previousInstanceType);
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                Set<Resource> resources = stack.getResources();
                List<CloudResource> cloudResources =
                        resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
                CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                cloudStack = cloudStackConverter.updateWithVerticalScaleRequest(cloudStack, stackVerticalScaleV4Request);
                Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
                CloudContext cloudContext = CloudContext.Builder.builder()
                        .withId(stack.getId())
                        .withName(stack.getName())
                        .withCrn(stack.getResourceCrn())
                        .withPlatform(stack.getCloudPlatform())
                        .withVariant(stack.getPlatformVariant())
                        .withLocation(location)
                        .withWorkspaceId(stack.getWorkspace().getId())
                        .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                        .build();

                CoreVerticalScaleRequest request = new CoreVerticalScaleRequest(cloudContext,
                        cloudCredential,
                        cloudStack,
                        cloudResources,
                        stackVerticalScaleV4Request);
                sendEvent(ctx, request);
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FINISHED_STATE")
    public Action<?, ?> stackVerticalScaleFinished() {
        return new AbstractClusterAction<>(CoreVerticalScaleResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CoreVerticalScaleResult payload, Map<Object, Object> variables) {
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                coreVerticalScaleService.updateTemplateWithVerticalScaleInformation(context.getStackId(), payload.getStackVerticalScaleV4Request(),
                        payload.getInstanceStorageCount(), payload.getInstanceStorageSize());
                coreVerticalScaleService.finishVerticalScale(context.getStackId(), payload.getStackVerticalScaleV4Request(), previousInstanceType);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(CoreVerticalScaleEvent.FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FAILED_STATE")
    public Action<?, ?> stackVerticalScaleFailedAction() {
        return new AbstractStackFailureAction<CoreVerticalScaleState, CoreVerticalScaleEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Exception during vertical scaling!: {}", payload.getException().getMessage());
                String groupBeingScaled = (String) variables.getOrDefault(GROUP_BEING_SCALED, "unknown");
                String previousInstanceType = (String) variables.getOrDefault(PREVIOUS_INSTANCE_TYPE, "unknown");
                String targetInstanceType = (String) variables.getOrDefault(TARGET_INSTANCE_TYPE, "unknown");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_FAILED.name(),
                        CLUSTER_VERTICALSCALED_FAILED,
                        groupBeingScaled, previousInstanceType, targetInstanceType, payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CoreVerticalScaleEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
