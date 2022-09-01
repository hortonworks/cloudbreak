package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerStopService;

@Component
public class GcpDatabaseServerStopService extends AbstractGcpDatabaseServerStartStopService implements DatabaseServerStopService {
    public void stop(AuthenticatedContext ac, DatabaseStack stack) throws Exception {
        startStop(ac, stack, "NEVER");
    }
}
