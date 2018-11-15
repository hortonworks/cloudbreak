package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

public abstract class AbstractStackDownscaleAction<P extends Payload>
        extends AbstractAction<StackDownscaleState, StackDownscaleEvent, StackScalingFlowContext, P> {
    protected static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    protected static final String INSTANCEIDS = "INSTANCEIDS";

    private static final String ADJUSTMENT = "ADJUSTMENT";

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackScalingService stackScalingService;

    protected AbstractStackDownscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(String flowId, StateContext<StackDownscaleState, StackDownscaleEvent> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        String instanceGroupName = extractInstanceGroupName(payload, variables);
        Set<String> instanceIds = extractInstanceIds(payload, variables, stack);
        Integer adjustment = extractAdjustment(payload, variables);
        CloudStack cloudStack = cloudStackConverter.convertForDownscale(stack, instanceIds);
        return new StackScalingFlowContext(flowId, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, instanceIds, adjustment);
    }

    private Integer extractAdjustment(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackDownscaleTriggerEvent) {
            StackDownscaleTriggerEvent ssc = (StackDownscaleTriggerEvent) payload;
            Integer adjustment = ssc.getPrivateIds() == null ? ssc.getAdjustment() : -ssc.getPrivateIds().size();
            variables.put(ADJUSTMENT, adjustment);
            return adjustment;
        }
        return getAdjustment(variables);
    }

    private Set<String> extractInstanceIds(P payload, Map<Object, Object> variables, Stack stack) {
        if (payload instanceof StackDownscaleTriggerEvent) {
            StackDownscaleTriggerEvent ssc = (StackDownscaleTriggerEvent) payload;
            Set<String> instanceIds;
            if (ssc.getPrivateIds() == null || ssc.getPrivateIds().isEmpty()) {
                Map<String, String> unusedInstanceIds = stackScalingService.getUnusedInstanceIds(ssc.getInstanceGroup(), ssc.getAdjustment(), stack);
                instanceIds = new HashSet<>(unusedInstanceIds.keySet());
            } else {
                Set<InstanceMetaData> imds = stack.getInstanceGroupByInstanceGroupName(ssc.getInstanceGroup()).getNotTerminatedInstanceMetaDataSet();
                instanceIds = imds.stream().filter(imd -> ssc.getPrivateIds().contains(imd.getPrivateId())).map(InstanceMetaData::getInstanceId)
                        .collect(Collectors.toSet());
            }
            variables.put(INSTANCEIDS, instanceIds);
            return instanceIds;
        }
        return getInstanceIds(variables);
    }

    private String extractInstanceGroupName(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackScaleTriggerEvent) {
            StackScaleTriggerEvent ssc = (StackScaleTriggerEvent) payload;
            variables.put(INSTANCEGROUPNAME, ssc.getInstanceGroup());
            return ssc.getInstanceGroup();
        }
        return getInstanceGroupName(variables);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }

    protected String getInstanceGroupName(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCEGROUPNAME);
    }

    protected Set<String> getInstanceIds(Map<Object, Object> variables) {
        return (Set<String>) variables.get(INSTANCEIDS);
    }

    protected Integer getAdjustment(Map<Object, Object> variables) {
        return (Integer) variables.get(ADJUSTMENT);
    }
}
