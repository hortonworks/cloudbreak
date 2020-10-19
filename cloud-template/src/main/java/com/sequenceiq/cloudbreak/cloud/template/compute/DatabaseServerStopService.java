package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

public interface DatabaseServerStopService {

    void stop(AuthenticatedContext ac, DatabaseStack stack) throws Exception;
}
