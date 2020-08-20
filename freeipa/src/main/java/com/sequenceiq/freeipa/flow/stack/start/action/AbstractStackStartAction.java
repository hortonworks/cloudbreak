package com.sequenceiq.freeipa.flow.stack.start.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartContext;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartState;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

public abstract class AbstractStackStartAction<P extends Payload> extends AbstractStackAction<StackStartState, StackStartEvent, StackStartContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackStartAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    protected AbstractStackStartAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackStartContext createFlowContext(FlowParameters flowParameters, StateContext<StackStartState, StackStartEvent> stateContext, P payload) {
        Long stackId = payload.getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        List<InstanceMetaData> instances = stack.getNotDeletedInstanceMetaDataList();
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.getCloudPlatform(), stack.getCloudPlatform(),
                location, stack.getOwner(), stack.getAccountId());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        return new StackStartContext(flowParameters, stack, instances, cloudContext, cloudCredential);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackStartContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}