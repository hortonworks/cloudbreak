package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.LAUNCH_STACK_FINISHED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;

@Component("MetadataCollectionFailedAction")
public class MetadataCollectionFailedAction extends AbstractStackCreationAction<CollectMetadataResult> {

    protected static final String TRIES = "METADATACOLLECTIONTRIES";
    protected static final Integer MAX_RETRIES = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageAction.class);

    @Inject
    private StackCreationService stackCreationService;
    @Inject
    private FlowMessageService flowMessageService;

    protected MetadataCollectionFailedAction() {
        super(CollectMetadataResult.class);
    }

    @Override
    protected void doExecute(StackContext context, CollectMetadataResult payload, Map<Object, Object> variables) throws Exception {
        Integer tries = Optional.fromNullable((Integer) variables.get(TRIES)).or(0);
        if (tries > MAX_RETRIES) {
            stackCreationService.handleStackCreationFailure(context, payload.getErrorDetails());
            sendEvent(context.getFlowId(), StackCreationEvent.STACK_CREATION_FAILE_HANDLED_EVENT.stringRepresentation(), payload);
        } else {
            LOGGER.error("Metadata collection failed.", payload.getErrorDetails());
            flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_INFRASTRUCTURE_METADATA_COLLECTION_FAILED, UPDATE_FAILED.name(),
                    payload.getErrorDetails().getMessage());
            variables.put(TRIES, tries + 1);
            LaunchStackResult launchStackResult = (LaunchStackResult) variables.get(LaunchStackResult.class);
            sendEvent(context.getFlowId(), LAUNCH_STACK_FINISHED_EVENT.stringRepresentation(), launchStackResult);
        }
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return null;
    }
}
