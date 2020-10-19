package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerStopService;

@Component
public class GcpDatabaseServerStopService extends AbstractGcpDatabaseServerStartStopService implements DatabaseServerStopService {

    @Inject
    private DatabasePollerService databasePollerService;

    public void stop(AuthenticatedContext ac, DatabaseStack stack) throws Exception {
        startStop(ac, stack, databasePollerService, "NEVER");
    }
}
