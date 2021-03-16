package com.sequenceiq.redbeams.flow.redbeams.common;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.store.RedbeamsInMemoryStateStoreUpdaterService;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RedbeamsInMemoryStateStoreUpdaterService redbeamsInMemoryStateStoreUpdaterService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload redbeamsPayload = (Payload) payload;
        DBStack dbStack = dbStackService.getById(redbeamsPayload.getResourceId());
        if (dbStack != null) {
            redbeamsInMemoryStateStoreUpdaterService.update(dbStack.getId(), dbStack.getStatus());
            MDCBuilder.buildMdcContext(dbStack);
            MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
            MDCBuilder.addAccountId(getAccountId(dbStack));
        }
        super.restart(flowParameters, flowChainId, event, payload);
    }

    private String getAccountId(DBStack dbStack) {
        return Optional.ofNullable(dbStack.getDatabaseServer()).map(DatabaseServer::getAccountId).orElse("");
    }
}
