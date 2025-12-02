package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;

@Component("RebuildBootstrapMachineAction")
public class RebuildBootstrapMachineAction extends AbstractRebuildAction<ClusterProxyRegistrationSuccess> {
    protected RebuildBootstrapMachineAction() {
        super(ClusterProxyRegistrationSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, ClusterProxyRegistrationSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Bootstrapping machines");
        BootstrapMachinesRequest request = new BootstrapMachinesRequest(payload.getResourceId());
        sendEvent(context, request);
    }

    @Override
    protected Object getFailurePayload(ClusterProxyRegistrationSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new BootstrapMachinesFailed(payload.getResourceId(), ex, ERROR);
    }
}
