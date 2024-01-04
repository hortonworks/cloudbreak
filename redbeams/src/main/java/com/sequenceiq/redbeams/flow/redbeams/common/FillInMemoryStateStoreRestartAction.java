package com.sequenceiq.redbeams.flow.redbeams.common;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.RestartContext;
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
    public void restart(RestartContext restartContext, Object payload) {
        DBStack dbStack = dbStackService.getById(restartContext.getResourceId());
        if (dbStack != null) {
            redbeamsInMemoryStateStoreUpdaterService.update(dbStack.getId(), dbStack.getStatus());
            MDCBuilder.buildMdcContext(dbStack);
            MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
            MDCBuilder.addAccountId(getAccountId(dbStack));
        }
        super.restart(restartContext, payload);
    }

    private String getAccountId(DBStack dbStack) {
        return Optional.ofNullable(dbStack.getDatabaseServer()).map(DatabaseServer::getAccountId).orElse("");
    }
}
