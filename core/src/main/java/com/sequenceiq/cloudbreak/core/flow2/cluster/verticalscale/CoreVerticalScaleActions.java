package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED_FAILED;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScalePreparationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Configuration
public class CoreVerticalScaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreVerticalScaleActions.class);

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

    @Bean(name = "STACK_PREPARATION_STATE")
    public Action<?, ?> stackPreparation() {
        return new AbstractClusterAction<>(CoreVerticalScalingTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, CoreVerticalScalingTriggerEvent payload, Map<Object, Object> variables)
                    throws Exception {
                StackVerticalScaleV4Request stackVerticalScaleV4Request = payload.getRequest();
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                InstanceGroupDto instanceGroup = stack.getInstanceGroupDtos().stream()
                        .filter(instance -> null != instance.getInstanceGroup().getGroupName() &&
                                instance.getInstanceGroup().getGroupName().equals(stackVerticalScaleV4Request.getGroup()))
                        .findFirst().get();
                LOGGER.debug("Updating status of stack for vertical scale.");
                coreVerticalScaleService.verticalScale(ctx.getStackId(), stackVerticalScaleV4Request);
                List<CloudResource> cloudResources = stack.getResources().stream().map(s -> cloudResourceConverter.convert(s))
                        .collect(Collectors.toList());
                CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());

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
                CoreVerticalScalePreparationRequest request = new CoreVerticalScalePreparationRequest(
                        cloudContext,
                        cloudCredential,
                        null,
                        stack,
                        instanceGroup,
                        cloudResources,
                        stackVerticalScaleV4Request);
                sendEvent(ctx, request);
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_STATE")
    public Action<?, ?> stackVerticalScale() {
        return new AbstractClusterAction<>(CoreVerticalScalePreparationResult.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, CoreVerticalScalePreparationResult payload, Map<Object, Object> variables)
                    throws Exception {
                StackVerticalScaleV4Request stackVerticalScaleV4Request = payload.getStackVerticalScaleV4Request();
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                InstanceGroupDto instanceGroup = stack.getInstanceGroupDtos().stream()
                        .filter(instance -> null != instance.getInstanceGroup().getGroupName() &&
                                instance.getInstanceGroup().getGroupName().equals(stackVerticalScaleV4Request.getGroup()))
                        .findFirst().get();
                Set<Resource> resources = stack.getResources();
                LOGGER.debug("Converting stack resources to cloud resources.");
                List<CloudResource> cloudResources =
                        resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
                CloudCredential cloudCredential = payload.getCloudCredential();
                if (!stack.isStackInStopPhase()) {
                    LOGGER.debug("Removing groups that aren't being vertically scaled from stack to convert to cloud stack.");
                    stack.getInstanceGroupDtos().stream()
                            .filter(instance -> !instance.getInstanceGroup().getGroupName().equals(stackVerticalScaleV4Request.getGroup())
                                    && !instance.getInstanceGroup().getInstanceGroupType().equals(InstanceGroupType.GATEWAY))
                            .map(instance -> instance.getInstanceGroup().getGroupName()).forEach(group -> stack.getInstanceGroups().remove(group));
                }
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                cloudStack = cloudStackConverter.updateWithVerticalScaleRequest(cloudStack, stackVerticalScaleV4Request);
                CloudContext cloudContext = payload.getCloudContext();

                CoreVerticalScaleRequest request = new CoreVerticalScaleRequest(stack,
                        instanceGroup,
                        payload.getGroupServiceComponents(),
                        payload.getInstanceStorageInfo(),
                        cloudContext,
                        cloudCredential,
                        cloudStack,
                        cloudResources,
                        stackVerticalScaleV4Request,
                        payload.getHostTemplateRoleGroupNames());
                sendEvent(ctx, request);
            }
        };
    }

    @Bean(name = "STACK_VERTICALSCALE_FINISHED_STATE")
    public Action<?, ?> stackVerticalScaleFinished() {
        return new AbstractClusterAction<>(CoreVerticalScaleResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CoreVerticalScaleResult payload, Map<Object, Object> variables) {
                coreVerticalScaleService.updateTemplateWithVerticalScaleInformation(context.getStackId(), payload.getStackVerticalScaleV4Request(),
                        payload.getInstanceStoreInfo());
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                coreVerticalScaleService.finishVerticalScale(context.getStackId(), payload.getStackVerticalScaleV4Request(), stack.isStackInStopPhase());
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
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_FAILED.name(),
                        CLUSTER_VERTICALSCALED_FAILED,
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CoreVerticalScaleEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
