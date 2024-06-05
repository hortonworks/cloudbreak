package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.cleanup.FreeIpaCleanupAfterRestoreRequest;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

/**
 * TODO
 * <a href="https://github.com/hortonworks/cloudbreak/blob/master/freeipa/src/main/resources/freeipa-salt/salt/freeipa/scripts/repair.sh">run cleanup</a>
 */
@Component("RebuildCleanupFreeIpaAfterRestoreAction")
public class RebuildCleanupFreeIpaAfterRestoreAction extends AbstractRebuildAction<HealthCheckSuccess> {
    protected RebuildCleanupFreeIpaAfterRestoreAction() {
        super(HealthCheckSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, HealthCheckSuccess payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new FreeIpaCleanupAfterRestoreRequest(payload.getResourceId()));
    }
}
