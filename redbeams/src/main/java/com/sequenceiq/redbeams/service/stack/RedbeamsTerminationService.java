package com.sequenceiq.redbeams.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationFlowConfig;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@Service
public class RedbeamsTerminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationService.class);

    private static final Long DEFAULT_WORKSPACE = 0L;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private FlowCancelService cancelService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private FlowLogService flowLogService;

    public DatabaseServerConfig terminateByCrn(String crn, boolean force) {
        return terminate(databaseServerConfigService.getByCrn(crn), force);
    }

    public DatabaseServerConfig terminateByName(String environmentCrn, String name, boolean force) {
        return terminate(databaseServerConfigService.getByName(DEFAULT_WORKSPACE, environmentCrn, name), force);
    }

    private DatabaseServerConfig terminate(DatabaseServerConfig server, boolean force) {
        String crn = server.getResourceCrn().toString();
        if (server.getResourceStatus() != ResourceStatus.SERVICE_MANAGED) {
            return databaseServerConfigService.deleteByCrn(crn);
        } else {
            DBStack dbStack = dbStackService.getByCrn(crn);
            MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());
            LOGGER.debug("Terminate called for: {} with force: {}", dbStack, force);

            if (flowLogService.isFlowConfigAlreadyRunning(dbStack.getId(), RedbeamsTerminationFlowConfig.class) || isAlreadyDeleted(server, dbStack)) {
                LOGGER.debug("DatabaseServer with crn {} is already being deleted", dbStack.getResourceCrn());
                return server;
            } else {
                dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.DELETE_REQUESTED);
                cancelService.cancelRunningFlows(dbStack.getId());
                flowManager.notify(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(),
                        new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_EVENT.selector(), dbStack.getId(), force));
                return databaseServerConfigService.getByCrn(crn);
            }
        }
    }

    private boolean isAlreadyDeleted(DatabaseServerConfig server, DBStack dbStack) {
        return server.isArchived() && dbStack.getStatus().isDeleteCompleted();
    }

    public Set<DatabaseServerConfig> terminateMultipleByCrn(Set<String> crns, boolean force) {
        Set<DatabaseServerConfig> resources = databaseServerConfigService.getByCrns(crns);
        return resources.stream()
                .map(server -> terminate(server, force))
                .collect(Collectors.toSet());
    }
}
