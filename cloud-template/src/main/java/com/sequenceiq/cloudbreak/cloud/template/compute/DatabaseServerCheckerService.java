package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

public interface DatabaseServerCheckerService {

    ExternalDatabaseStatus check(AuthenticatedContext ac, DatabaseStack dbStacks);
}
