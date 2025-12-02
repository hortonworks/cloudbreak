package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildState;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public abstract class AbstractRebuildAction<P extends Payload>
        extends AbstractStackAction<FreeIpaRebuildState, FreeIpaRebuildFlowEvent, StackContext, P>
        implements OperationAwareAction {

    private static final String INSTANCE_TO_RESTORE = "INSTANCE_TO_RESTORE";

    private static final String FULL_BACKUP_LOCATION = "FULL_BACKUP_LOCATION";

    private static final String DATA_BACKUP_LOCATION = "DATA_BACKUP_LOCATION";

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackUpdater stackUpdater;

    protected AbstractRebuildAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaRebuildState, FreeIpaRebuildFlowEvent> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        CloudContext cloudContext = buildContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new RebuildFailureEvent(payload.getResourceId(), ERROR, ex);
    }

    protected void setInstanceToRestoreFqdn(Map<Object, Object> variables, String fqdn) {
        variables.put(INSTANCE_TO_RESTORE, fqdn);
    }

    protected void setFullBackupStorageLocation(Map<Object, Object> variables, String location) {
        variables.put(FULL_BACKUP_LOCATION, location);
    }

    protected void setDataBackupStorageLocation(Map<Object, Object> variables, String location) {
        variables.put(DATA_BACKUP_LOCATION, location);
    }

    protected String getInstanceToRestoreFqdn(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCE_TO_RESTORE);
    }

    protected String getFullBackupStorageLocation(Map<Object, Object> variables) {
        return (String) variables.get(FULL_BACKUP_LOCATION);
    }

    protected String getDataBackupStorageLocation(Map<Object, Object> variables) {
        return (String) variables.get(DATA_BACKUP_LOCATION);
    }

    protected StackUpdater stackUpdater() {
        return stackUpdater;
    }
}
