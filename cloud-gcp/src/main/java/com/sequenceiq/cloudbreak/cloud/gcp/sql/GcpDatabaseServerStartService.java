package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.poller.DatabasePollerService;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerStartService;

@Component
public class GcpDatabaseServerStartService extends AbstractGcpDatabaseServerStartStopService implements DatabaseServerStartService {

    @Inject
    private DatabasePollerService databasePollerService;

    public void start(AuthenticatedContext ac, DatabaseStack stack) throws Exception {
        startStop(ac, stack, databasePollerService, "ALWAYS");
    }
}
